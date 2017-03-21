package server.controllers;


import network.Message;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.Constants;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.*;

import java.io.DataOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;

public class TransactionController {

    public static void makeTransaction() {
        path("/transact", () -> {
            get("", (request, response) -> {
                Map<String, Object> emptyModel = new HashMap<>();
                return new ModelAndView(emptyModel, "transact.ftl");
            }, new FreeMarkerEngine());

            post("", (request, response) -> {
                // TODO missing a lot of null checks, etc.
                int transactionIndex = request.queryMap("index").integerValue();
                long amount = request.queryMap("amount").longValue();

                byte[] senderPublicKey = ByteUtil.hexStringToByteArray(
                        request.queryParams("senderpublickey")).get();
                byte[] recipientPublicKeyBytes = ByteUtil.hexStringToByteArray(
                        request.queryParams("recipientpublickey")).get();
                ShaTwoFiftySix inputHash = ShaTwoFiftySix.create(
                        ByteUtil.hexStringToByteArray(request.queryParams("transaction")).get())
                        .get();

                User user = UserAccess.getUserbyUsername(request.session().attribute("username"));
                if (user == null) {
                    // TODO handle
                    return "not logged in";
                }
                Key senderKey = UserAccess.getKey(user.getId(), senderPublicKey);
                if (senderKey == null) {
                    // TODO handle
                    return "wrong user";
                }
                PublicKey recipientPublicKey = Crypto.parsePublicKey(recipientPublicKeyBytes);
                PrivateKey senderPrivateKey = Crypto.parsePrivateKey(senderKey.getEncryptedPrivateKey());

                Transaction transaction = new Transaction.Builder()
                        .addInput(new TxIn(inputHash, transactionIndex), senderPrivateKey)
                        .addOutput(new TxOut(amount, recipientPublicKey))
                        .build();

                byte[] payload = ByteUtil.asByteArray(transaction::serializeWithSignatures);
                try (Socket socket = new Socket(
                        Constants.getNodeAddress().getAddress(),
                        Constants.getNodeAddress().getPort())) {
                    DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
                    IOUtils.sendMessage(socketOut, Message.TRANSACTION, payload);
                }
                return "ok";
            });
        });
    }
}
