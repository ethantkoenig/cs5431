package server.controllers;


import com.google.inject.Inject;
import crypto.ECDSAPublicKey;
import crypto.ECDSASignature;
import network.Message;
import network.OutgoingMessage;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.Constants;
import server.utils.RouteUtils;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Optional;

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

        int index = queryParamInt(request, "index");
        long amount = queryParamLong(request, "amount");
        byte[] senderPublicKey = queryParamHex(request, "senderpublickey");
        byte[] recipientPublicKeyBytes = queryParamHex(request, "recipientpublickey");
        ShaTwoFiftySix inputHash = ShaTwoFiftySix.create(
                queryParamHex(request, "transaction")
        ).orElseThrow(InvalidParamException::new);

        User loggedInUser = routeUtils.forceLoggedInUser(request);

        Optional<Key> senderKey = userAccess.getKey(loggedInUser.getId(), senderPublicKey);
        if (!senderKey.isPresent()) {
            // TODO handle
            response.status(400);
            return "no such key under user";
        }

        ECDSAPublicKey recipientPublicKey = ECDSAPublicKey.DESERIALIZER.deserialize(
                recipientPublicKeyBytes
        );

        Transaction transaction = new Transaction.UnsignedBuilder()
                .addInput(new TxIn(inputHash, index))
                .addOutput(new TxOut(amount, recipientPublicKey))
                .build();

        byte[] payload = ByteUtil.asByteArray(transaction::serialize);
        response.status(200);
        return String.format("{\"payload\":\"%s\", \"encryptedKey\":%s}",
                ByteUtil.bytesToHexString(payload),
                senderKey.get().encryptedPrivateKey
        );
    }

    String sendTransaction(Request request, Response response) throws Exception {
        byte[] payload = queryParamHex(request, "payload");
        String rHexString = queryParam(request, "r");
        String sHexString = queryParam(request, "s");

        BigInteger r = new BigInteger(rHexString, 16);
        BigInteger s = new BigInteger(sHexString, 16);
        ECDSASignature signature = new ECDSASignature(r, s);

        byte[] msgPayload = ByteUtil.asByteArray(outputStream -> {
            outputStream.write(payload);
            signature.serialize(outputStream);
        });

        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {
            DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
            new OutgoingMessage(Message.TRANSACTION, msgPayload).serialize(socketOut);
        }
        return "ok"; // TODO
    }
}
