package server.controllers;


import network.Message;
import network.OutgoingMessage;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.Constants;
import server.utils.RouteUtils;
import spark.template.freemarker.FreeMarkerEngine;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;

public class TransactionController {

    public static void makeTransaction() {
        path("/transact", () -> {
            get("", (request, response) ->
                            RouteUtils.modelAndView(request, "transact.ftl")
                                    .get()
                    , new FreeMarkerEngine());

            post("", wrapRoute((request, response) -> {
                int index = queryParamInt(request, "index");
                long amount = queryParamLong(request, "amount");
                byte[] senderPublicKey = queryParamHex(request, "senderpublickey");
                byte[] recipientPublicKeyBytes = queryParamHex(request, "recipientpublickey");
                ShaTwoFiftySix inputHash = ShaTwoFiftySix.create(
                        queryParamHex(request, "transaction")
                ).orElseThrow(InvalidParamException::new);

                User loggedInUser = forceLoggedInUser(request);
                Key senderKey = UserAccess.getKey(loggedInUser.getId(), senderPublicKey);
                if (senderKey == null) {
                    // TODO handle
                    response.status(400);
                    return "no such key under user";
                }

                PublicKey recipientPublicKey;
                PrivateKey senderPrivateKey;

                try {
                    recipientPublicKey = Crypto.parsePublicKey(recipientPublicKeyBytes);
                    senderPrivateKey = Crypto.parsePrivateKey(senderKey.getEncryptedPrivateKey());
                } catch (GeneralSecurityException e) {
                    // TODO handle
                    response.status(400);
                    return "bad keys";
                }

                Transaction transaction = new Transaction.Builder()
                        .addInput(new TxIn(inputHash, index), senderPrivateKey)
                        .addOutput(new TxOut(amount, recipientPublicKey))
                        .build();
                sendTransaction(transaction);
                return "ok";
            }));
        });
    }

    private static void sendTransaction(Transaction transaction) throws IOException {
        byte[] payload = ByteUtil.asByteArray(transaction::serialize);
        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {
            DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
            new OutgoingMessage(Message.TRANSACTION, payload).serialize(socketOut);
        }
    }
}
