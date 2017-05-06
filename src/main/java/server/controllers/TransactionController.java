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
import server.models.Key;
import server.models.Transaction;
import server.models.User;
import server.utils.Constants;
import server.utils.RouteUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Optionals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;

public class TransactionController extends AbstractController {

    private final UserAccess userAccess;
    private final KeyAccess keyAccess;
    private final TransactionAccess transactionAccess;
    private final RouteUtils routeUtils;

    @Inject
    private TransactionController(UserAccess userAccess, KeyAccess keyAccess, TransactionAccess transactionAccess, RouteUtils routeUtils) {
        this.userAccess = userAccess;
        this.keyAccess = keyAccess;
        this.transactionAccess = transactionAccess;
        this.routeUtils = routeUtils;
    }

    public void init() {
        path("/transact", () -> {
            get("", wrapTemplate(this::getTransact), new FreeMarkerEngine());
            post("", wrapRoute(this::transact));
        });

        path("/requests", () -> {
            get("", wrapTemplate(this::getRequests), new FreeMarkerEngine());
            post("", wrapRoute(this::createRequest));
        });

        post("/sendtransaction", wrapRoute(this::sendTransaction));
    }

    ModelAndView getTransact(Request request, Response response) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String username = loggedInUser.getUsername();
        List<String> friends = userAccess.getPeopleWhoFriendMe(username);
        List<Transaction> transactions = transactionAccess.getAllTransactions(username);

        return routeUtils.modelAndView(request, "transact.ftl")
                .add("friends", friends)
                .add("transactions", transactions)
                .get();
    }

    String transact(Request request, Response response) throws Exception {
        response.type("application/json");

        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String recipientUsername = queryParam(request, "recipient");
        String message = queryParam(request, "message");

        if (!userAccess.isFriendsWith(recipientUsername, loggedInUser.getUsername())) {
            return "This person has not authorized you to send them money.";
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
        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {

            new GetUTXWithKeysRequestPayload(
                    keys,
                    keys.get(0),
                    recipientKeys.get(0),
                    amount
            ).toMessage().serialize(new DataOutputStream(socket.getOutputStream()));

            IncomingMessage respMessage = IncomingMessage.responderlessDeserializer()
                    .deserialize(new DataInputStream(socket.getInputStream()));
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

        // if transaction already exists (meaning it was a requesut) mark as complete
        if (queryParamExists(request, "tranid")) {
            int tranid = queryParamInt(request, "tranid");
            transactionAccess.updateTransactionRequestAsComplete(tranid, loggedInUser.getUsername());
        } else {
            transactionAccess.insertTransaction(loggedInUser.getUsername(), recipientUsername, amount, message, false);
        }

        // TODO find a better way to produce JSON
        response.status(200);
        return String.format("{\"payload\":\"%s\", \"encryptedKeys\":[%s]}",
                ByteUtil.bytesToHexString(payload),
                String.join(", ", encryptedPrivateKeys)
        );
    }

    String sendTransaction(Request request, Response response) throws Exception {
        byte[] payload = queryParamHex(request, "payload");
        String[] rHexs = queryParam(request, "r").split(",");
        String[] sHexs = queryParam(request, "s").split(",");

        if (rHexs.length != sHexs.length) {
            return "mismatched lengths"; // TODO handle properly
        }

        byte[] msgPayload = ByteUtil.asByteArray(outputStream -> {
            outputStream.write(payload);
            for (int i = 0; i < rHexs.length; i++) {
                BigInteger r = new BigInteger(rHexs[i], 16);
                BigInteger s = new BigInteger(sHexs[i], 16);
                new ECDSASignature(r, s).serialize(outputStream);
            }
        });

        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {
            DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
            new OutgoingMessage(Message.TRANSACTION, msgPayload).serialize(socketOut);
        }
        return "ok"; // TODO handle properly
    }

    ModelAndView getRequests(Request request, Response response) throws Exception {
        User touser = routeUtils.forceLoggedInUser(request);
        List<Transaction> requests = transactionAccess.getRequests(touser.getUsername());
        return routeUtils.modelAndView(request, "request.ftl")
                .add("requests", requests)
                .get();
    }

    String createRequest(Request request, Response response) throws Exception {
        User touser = routeUtils.forceLoggedInUser(request);
        String fromuser = queryParam(request, "recipient");

        if (!userAccess.isFriendsWith(fromuser, touser.getUsername())) {
            return "This person has not authorized you to send them money.";
        }

        String message = queryParam(request, "message");

        if (message.length() > 250) {
            return "Message too long, must be under 250 characters";
        }

        long amount = queryParamLong(request, "amount");

        transactionAccess.insertTransaction(fromuser, touser.getUsername(), amount, message, true);
        return "Request made.";
    }
}
