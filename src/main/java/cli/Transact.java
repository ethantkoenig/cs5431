package cli;


import network.Message;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Crypto;
import utils.IOUtils;
import utils.ShaTwoFiftySix;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

public class Transact {

    private final BufferedReader input;

    private Transact() {
        input = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
    }

    public static void run(String[] args) throws IOException {
        if (args.length < 3 || args.length % 2 != 1) {
            System.err.println("usage: transact [<IP address> <port number>]");
            System.exit(1);
        }

        Socket[] sockets = new Socket[(args.length - 1) / 2];
        for (int i = 0; i < sockets.length; i++) {
            sockets[i] = new Socket(
                    InetAddress.getByName(args[2 * i + 1]),
                    Integer.parseInt(args[2 * i + 2])
            );
        }

        Transact transact = new Transact();
        try {
            transact.runTransaction(sockets);
            transact.input.close();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void runTransaction(Socket[] sockets) throws GeneralSecurityException, IOException {
        RTransaction.Builder builder = new RTransaction.Builder();
        getInputs(builder);
        getOutputs(builder);
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
                    new RTxIn(ShaTwoFiftySix.create(IOUtils.parseHex(hexHash)), index),
                    Crypto.loadPrivateKey(privateFilename)
            );
        }
    }

    private void getOutputs(RTransaction.Builder builder)
            throws IOException, GeneralSecurityException {
        int numOutputs = (int) promptUserInt("Number of outputs");
        for (int i = 0; i < numOutputs; i++) {
            System.out.println(String.format("[Output %d]", i));
            String hexHash = promptUser("Public key of recipient");
            long amount = promptUserInt("Amount to send to recipient");

            builder.addOutput(new RTxOut(amount, Crypto.parsePublicKey(IOUtils.parseHex(hexHash))));
        }
    }

    private long promptUserInt(String prompt) throws IOException {
        String text = promptUser(prompt);
        try {
            long n = Long.parseLong(text);
            if (n <= 0) {
                throw new InvalidInputException("Number must be positive");
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
