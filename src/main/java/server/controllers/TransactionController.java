package server.controllers;


import com.google.inject.Inject;
import crypto.ECDSAPublicKey;
import crypto.ECDSASignature;
import network.*;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.Constants;
import server.utils.RouteUtils;
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

public class TransactionController {

    private final UserAccess userAccess;
    private final RouteUtils routeUtils;

    @Inject
    private TransactionController(UserAccess userAccess, RouteUtils routeUtils) {
        this.userAccess = userAccess;
        this.routeUtils = routeUtils;
    }

    public void init() {
        makeTransaction();
    }

    private void makeTransaction() {
        path("/transact", () -> {
            get("", routeUtils.template("transact.ftl"), new FreeMarkerEngine());
            post("", wrapRoute(this::transact));
        });

        post("/sendtransaction", wrapRoute(this::sendTransaction));
    }

    String transact(Request request, Response response) throws Exception {
        response.type("application/json");

        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String recipientUsername = queryParam(request, "recipient");
        Optional<User> recipient = userAccess.getUserbyUsername(recipientUsername);
        if (!recipient.isPresent()) {
            return "invalid recipient"; // TODO handle properly
        }
        List<ECDSAPublicKey> keys = userAccess.getKeysByUserID(loggedInUser.getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());
        List<ECDSAPublicKey> recipientKeys = userAccess.getKeysByUserID(recipient.get().getId()).stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());
        long amount = queryParamLong(request, "amount");

        if (keys.isEmpty() || recipientKeys.isEmpty()) {
            return "oh no, no keys"; // TODO handle properly
        }

        GetUTXWithKeysResponse unsigned;
        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {

            byte[] payload = ByteUtil.asByteArray(new GetUTXWithKeysRequest(
                    keys,
                    keys.get(0),
                    recipientKeys.get(0),
                    amount)::serialize);
            new OutgoingMessage(Message.GET_UTX_WITH_KEYS, payload)
                    .serialize(new DataOutputStream(socket.getOutputStream()));

            IncomingMessage respMessage = IncomingMessage.responderlessDeserializer()
                    .deserialize(new DataInputStream(socket.getInputStream()));
            if (respMessage.type != Message.UTX_WITH_KEYS) {
                return "oh no, bad response from node"; // TODO handle properly
            }
            unsigned = GetUTXWithKeysResponse.DESERIALIZER.deserialize(respMessage.payload);
        }

        if (!unsigned.wasSuccessful) {
            // TODO don't have enough money, handle properly
            return "oh no, not successful! (you don't have enough money)";
        }
        byte[] payload = unsigned.unsignedTransaction;

        List<String> encryptedPrivateKeys = new ArrayList<>();
        for (ECDSAPublicKey publicKey : unsigned.keysUsed) {
            byte[] serialized = ByteUtil.asByteArray(publicKey::serialize);
            userAccess.getKey(loggedInUser.getId(), serialized)
                    .ifPresent(k -> encryptedPrivateKeys.add(k.encryptedPrivateKey));
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
}
