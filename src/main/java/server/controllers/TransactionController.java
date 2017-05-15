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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        long amount = queryParamLong(request, "amount");
        String message = queryParam(request, "message");

        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidParamException(MESSAGE_TOO_LONG);
        }
        User recipient = userAccess.getUserByUsername(recipientUsername)
                .orElseThrow(() -> new InvalidParamException("Invalid recipient"));

        if (!userAccess.isFriendsWith(recipientUsername, loggedInUser.getUsername())) {
            LOGGER.info("Unauthorized transaction; user=%d, recipient=%d, amount=%d, message=%s",
                    loggedInUser.getId(), recipient.getId(), amount, message);
            throw new InvalidParamException("This person has not authorized you to send them money.");
        }

        List<ECDSAPublicKey> keys = keyAccess.getKeysByUserID(loggedInUser.getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());
        List<ECDSAPublicKey> recipientKeys = keyAccess.getKeysByUserID(recipient.getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());

        if (keys.isEmpty()) {
            throw new InvalidParamException("You have not uploaded any keys");
        } else if (recipientKeys.isEmpty()) {
            throw new InvalidParamException("The recipient has not uploaded any keys");
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
                LOGGER.severe("Unexpected message type: %d", respMessage.type);

            }
            unsigned = GetUTXWithKeysResponsePayload.DESERIALIZER.deserialize(respMessage.payload);
        }

        if (!unsigned.wasSuccessful) {
            throw new InvalidParamException("You do not have sufficient funds for this transaction");
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
        if (queryParamExists(request, "tranId")) {
            int tranid = queryParamInt(request, "tranId");
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
        return "ok";
    }

    ModelAndView getRequests(Request request, Response response, Log log) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        List<Transaction> requests = transactionAccess.getRequests(loggedInUser.getUsername());

        Set<String> friendedMe = new HashSet<>(
                userAccess.getPeopleWhoFriendMe(loggedInUser.getUsername())
        );
        List<String> friends = userAccess.getFriends(loggedInUser.getUsername()).stream()
                .filter(friendedMe::contains).collect(Collectors.toList());
        return routeUtils.modelAndView(request, "request.ftl")
                .add("friends", friends)
                .add("requests", requests)
                .get();
    }

    String createRequest(Request request, Response response, Log log) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String requestee = queryParam(request, "requestee");
        long amount = queryParamLong(request, "amount");
        String message = queryParam(request, "message");

        if (!userAccess.isFriendsWith(loggedInUser.getUsername(), requestee)) {
            LOGGER.info("Unauthorized request; user=%d, requstee=%s, amount=%d, message=%s",
                    loggedInUser.getId(), requestee, amount, message);
            throw new InvalidParamException("You must authorize the requestee to send you money.");
        } else if (!userAccess.isFriendsWith(requestee, loggedInUser.getUsername())) {
            LOGGER.info("Unauthorized request; user=%d, requstee=%s, amount=%d, message=%s",
                    loggedInUser.getId(), requestee, amount, message);
            throw new InvalidParamException("This person has not authorized you to make requests.");
        }

        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidParamException(MESSAGE_TOO_LONG);
        }

        transactionAccess.insertTransaction(requestee, loggedInUser.getUsername(), amount, message, true);
        log.info("Created request; requester=%d, requestee=%s, amount=%d, message=%s",
                loggedInUser.getId(), requestee, amount, message);
        response.redirect("/user");
        return "redirected";
    }

    String deleteRequest(Request request, Response response, Log log) throws Exception {
        User user = routeUtils.forceLoggedInUser(request);
        int transactionId = queryParamInt(request, "tranId");
        log.info("Delete request; id=%d", transactionId);
        transactionAccess.deleteRequest(transactionId, user.getUsername());
        return "ok";
    }
}
