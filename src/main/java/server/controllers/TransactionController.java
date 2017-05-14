package server.controllers;


import com.google.inject.Inject;
import crypto.ECDSAPublicKey;
import crypto.ECDSASignature;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.GetUTXWithKeysRequestPayload;
import message.payloads.GetUTXWithKeysResponsePayload;
import server.access.KeyAccess;
import server.access.TransactionAccess;
import server.access.UserAccess;
import server.bodies.SendTransactionBody;
import server.bodies.TransactionResponseBody;
import server.models.Key;
import server.models.Transaction;
import server.models.User;
import server.utils.CryptocurrencyEndpoint;
import server.utils.RouteUtils;
import server.utils.RouteWrapper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Log;
import utils.Optionals;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;
import static utils.ByteUtil.bytesToHexString;

public class TransactionController extends AbstractController {
    private static final Log LOGGER = Log.forClass(TransactionController.class);
    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final String MESSAGE_TOO_LONG =
            "Message too long, must be under " + MAX_MESSAGE_LENGTH + " characters";


    private final UserAccess userAccess;
    private final KeyAccess keyAccess;
    private final TransactionAccess transactionAccess;
    private final RouteUtils routeUtils;
    private final CryptocurrencyEndpoint.Provider endpointProvider;

    @Inject
    private TransactionController(UserAccess userAccess,
                                  KeyAccess keyAccess,
                                  TransactionAccess transactionAccess,
                                  RouteUtils routeUtils,
                                  CryptocurrencyEndpoint.Provider endpointProvider) {
        this.userAccess = userAccess;
        this.keyAccess = keyAccess;
        this.transactionAccess = transactionAccess;
        this.routeUtils = routeUtils;
        this.endpointProvider = endpointProvider;
    }

    public void init() {
        RouteWrapper wrapper = new RouteWrapper(LOGGER);
        path("/transact", () -> {
            get("", wrapper.template(this::getTransact), new FreeMarkerEngine());
            post("", wrapper.route(this::transact));
        });

        path("/requests", () -> {
            get("", wrapper.template(this::getRequests), new FreeMarkerEngine());
            post("", wrapper.route(this::createRequest));
            delete("", wrapper.route(this::deleteRequest));
        });

        post("/sendtransaction", wrapper.route(this::sendTransaction));
    }

    ModelAndView getTransact(Request request, Response response, Log log) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String username = loggedInUser.getUsername();
        List<String> friends = userAccess.getPeopleWhoFriendMe(username);
        List<Transaction> transactions = transactionAccess.getAllTransactions(username);

        return routeUtils.modelAndView(request, "transact.ftl")
                .add("friends", friends)
                .add("transactions", transactions)
                .get();
    }

    String transact(Request request, Response response, Log log) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String recipientUsername = queryParam(request, "recipient");
        String message = queryParam(request, "message");
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidParamException(MESSAGE_TOO_LONG);
        }

        if (!userAccess.isFriendsWith(recipientUsername, loggedInUser.getUsername())) {
            return "This person has not authorized you to send them money."; // TODO
        }

        Optional<User> recipient = userAccess.getUserByUsername(recipientUsername);
        if (!recipient.isPresent()) {
            return "invalid recipient"; // TODO handle properly
        }
        List<ECDSAPublicKey> keys = keyAccess.getKeysByUserID(loggedInUser.getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());
        List<ECDSAPublicKey> recipientKeys = keyAccess.getKeysByUserID(recipient.get().getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());
        long amount = queryParamLong(request, "amount");

        if (keys.isEmpty() || recipientKeys.isEmpty()) {
            return "oh no, no keys"; // TODO handle properly
        }

        GetUTXWithKeysResponsePayload unsigned;
        try (CryptocurrencyEndpoint endpoint = endpointProvider.getEndpoint()) {
            endpoint.send(new GetUTXWithKeysRequestPayload(
                    keys,
                    keys.get(0),
                    recipientKeys.get(0),
                    amount
            ).toMessage());

            IncomingMessage respMessage = endpoint.receive();
            if (respMessage.type != Message.UTX_WITH_KEYS) {
                return "oh no, bad response from node"; // TODO handle properly
            }
            unsigned = GetUTXWithKeysResponsePayload.DESERIALIZER.deserialize(respMessage.payload);
        }

        if (!unsigned.wasSuccessful) {
            // TODO don't have enough money, handle properly
            return "oh no, not successful! (you don't have enough money)";
        }
        byte[] payload = unsigned.unsignedTransaction;

        List<String> encryptedPrivateKeys = new ArrayList<>();
        for (ECDSAPublicKey publicKey : unsigned.keysUsed) {
            byte[] serialized = ByteUtil.asByteArray(publicKey::serialize);
            keyAccess.getKey(loggedInUser.getId(), serialized)
                    .ifPresent(k -> encryptedPrivateKeys.add(k.encryptedPrivateKey));
        }

        ShaTwoFiftySix payloadHash = ShaTwoFiftySix.hashOf(payload);
        // if transaction already exists (meaning it was a request) mark as complete
        if (queryParamExists(request, "tranid")) {
            int tranid = queryParamInt(request, "tranid");
            transactionAccess.updateTransactionRequestAsComplete(tranid, loggedInUser.getUsername());
            log.info("Accepted request; user=%d, tranId=%d, payloadHash=%s",
                    loggedInUser.getId(), tranid, payloadHash);
        } else {
            transactionAccess.insertTransaction(loggedInUser.getUsername(), recipientUsername, amount, message, false);
            log.info("Created transaction; user=%d, payloadHash=%s",
                    loggedInUser.getId(), payloadHash);
        }

        response.status(200);
        return routeUtils.toJson(response, new TransactionResponseBody(
                bytesToHexString(payload),
                encryptedPrivateKeys
        ));
    }

    String sendTransaction(Request request, Response response, Log log) throws Exception {
        SendTransactionBody payload = routeUtils.parseBody(request, SendTransactionBody.class);

        byte[] msgPayload = ByteUtil.asByteArray(outputStream -> {
            outputStream.write(payload.payload());
            for (ECDSASignature signature : payload.signatures()) {
                signature.serialize(outputStream);
            }
        });

        try (CryptocurrencyEndpoint endpoint = endpointProvider.getEndpoint()) {
            endpoint.send(new OutgoingMessage(Message.TRANSACTION, msgPayload));
        }
        successMessage(request, "Transaction sent!");
        return "ok"; // TODO handle properly
    }

    ModelAndView getRequests(Request request, Response response, Log log) throws Exception {
        User touser = routeUtils.forceLoggedInUser(request);
        List<Transaction> requests = transactionAccess.getRequests(touser.getUsername());
        List<String> friends = userAccess.getFriends(touser.getUsername());
        return routeUtils.modelAndView(request, "request.ftl")
                .add("friends", friends)
                .add("requests", requests)
                .get();
    }

    String createRequest(Request request, Response response, Log log) throws Exception {
        User touser = routeUtils.forceLoggedInUser(request);
        String fromuser = queryParam(request, "requestee");

        if (!userAccess.isFriendsWith(fromuser, touser.getUsername())) {
            return "This person has not authorized you to send them money.";
        }

        String message = queryParam(request, "message");
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidParamException(MESSAGE_TOO_LONG);
        }

        long amount = queryParamLong(request, "amount");

        transactionAccess.insertTransaction(fromuser, touser.getUsername(), amount, message, true);
        log.info("Created request; touser=%s, fromuser=%s, amount=%d, message=%s",
                touser, fromuser, amount, message);
        response.redirect("/user");
        return "redirected";
    }

    String deleteRequest(Request request, Response response, Log log) throws Exception {
        User user = routeUtils.forceLoggedInUser(request);
        int transactionId = queryParamInt(request, "tranid");
        log.info("Delete request; id=%d", transactionId);
        transactionAccess.deleteRequest(transactionId, user.getUsername());
        return "ok";
    }
}
