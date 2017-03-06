package cli;


import network.Message;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.ByteUtil;
import utils.Crypto;
import utils.IOUtils;
import utils.ShaTwoFiftySix;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class Transact {

    private final BufferedReader input;

    private Transact(BufferedReader input) {
        this.input = input;
    }

    public static void run(BufferedReader input, String nodeListPath) throws GeneralSecurityException, IOException {
        InputStreamReader streamIn =
                new InputStreamReader(new FileInputStream(nodeListPath), Charset.defaultCharset());
        BufferedReader nodeReader = new BufferedReader(streamIn);
        List<Socket> sockets = new ArrayList<Socket>();
        String line;
        Transact transact = null;
        try {
            while ((line = nodeReader.readLine()) != null) {
                String[] IPandPort = line.split(" ");
                // Check to ensure we only have IP and Port
                if (IPandPort.length == 2) {
                    sockets.add(new Socket(
                            InetAddress.getByName(IPandPort[0]),
                            Integer.parseInt(IPandPort[1]))
                    );
                }
            }
            transact = new Transact(input);
            transact.runTransaction(sockets);
        } finally {
            nodeReader.close();
            streamIn.close();
        }
    }

    private void runTransaction(List<Socket> sockets) throws GeneralSecurityException, IOException {
        RTransaction.Builder builder = new RTransaction.Builder();
        try {
            getInputs(builder);
            getOutputs(builder);
        } catch (InvalidInputException e) {
            System.err.println(e.getMessage());
            return;
        }
        RTransaction transaction = builder.build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transaction.serializeWithSignatures(new DataOutputStream(outputStream));
        byte[] payload = outputStream.toByteArray();

        for (Socket socket : sockets) {
            DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
            IOUtils.sendMessage(socketOut, Message.TRANSACTION, payload);
            socketOut.close();
            socket.close();
        }
    }

    private void getInputs(RTransaction.Builder builder)
            throws IOException, GeneralSecurityException {
        int numInputs = (int) promptUserInt("Number of inputs");
        for (int i = 0; i < numInputs; i++) {
            System.out.println(String.format("[Input %d]", i));
            String hexHash = promptUser("Hash of input transaction");
            int index = (int) promptUserInt("Index of input funds");
            String privateFilename = promptUser("Filename for private key");

            builder.addInput(
                    new RTxIn(ShaTwoFiftySix.create(ByteUtil.hexStringToByteArray(hexHash)), index),
                    Crypto.loadPrivateKey(privateFilename)
            );
        }
    }

    private void getOutputs(RTransaction.Builder builder)
            throws IOException, GeneralSecurityException {
        int numOutputs = (int) promptUserInt("Number of outputs");
        for (int i = 0; i < numOutputs; i++) {
            System.out.println(String.format("[Output %d]", i));
            String publicKeyFilename = promptUser("Public key of recipient");
            long amount = promptUserInt("Amount to send to recipient");

            builder.addOutput(new RTxOut(amount, Crypto.loadPublicKey(publicKeyFilename)));
        }
    }

    private long promptUserInt(String prompt) throws IOException {
        String text = promptUser(prompt);
        try {
            long n = Long.parseLong(text);
            if (n < 0) {
                throw new InvalidInputException("Number must be non-negative");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Invalid number");
        }
    }

    private String promptUser(String prompt) throws IOException {
        System.out.print(String.format("%s: ", prompt));
        return input.readLine();
    }

    private static class InvalidInputException extends RuntimeException {
        private InvalidInputException(String message) {
            super(message);
        }
    }
}
