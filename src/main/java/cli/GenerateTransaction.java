package cli;


import network.Message;
import network.OutgoingMessage;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Crypto;
import utils.IOUtils;
import utils.ShaTwoFiftySix;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class GenerateTransaction {

    private final BufferedReader input;

    private GenerateTransaction(BufferedReader input) {
        this.input = input;
    }

    /**
     * Constructs a transaction based on the content read from {@code input},
     * and sends the transaction to each socket address in {@code socketAddresses}
     *
     * @param input           presumably a wrapper around {@code System.in}, will be read
     *                        to construct the transaction.
     * @param socketAddresses list of addresses to send transaction to
     */
    public static void run(BufferedReader input, List<InetSocketAddress> socketAddresses)
            throws GeneralSecurityException, IOException {
        new GenerateTransaction(input).runTransaction(socketAddresses);
    }

    private void runTransaction(List<InetSocketAddress> addresses) throws GeneralSecurityException, IOException {
        Optional<Transaction> transaction = getTransaction();
        if (!transaction.isPresent()) {
            return;
        }
        sendTransaction(transaction.get(), addresses);

        System.out.println(String.format("Successfully sent transaction: %s",
                transaction.get().getShaTwoFiftySix()));
    }

    private Optional<Transaction> getTransaction() throws GeneralSecurityException, IOException {
        Transaction.Builder builder = new Transaction.Builder();
        try {
            getInputs(builder);
            getOutputs(builder);
        } catch (InvalidInputException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
        return Optional.of(builder.build());
    }

    private void sendTransaction(Transaction transaction, List<InetSocketAddress> addresses)
            throws IOException {
        byte[] payload = ByteUtil.asByteArray(transaction::serializeWithSignatures);

        for (InetSocketAddress address : addresses) {
            try (Socket socket = new Socket(address.getAddress(), address.getPort())) {
                DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
                new OutgoingMessage(Message.TRANSACTION, payload).serialize(socketOut);
            }
        }
    }

    private void getInputs(Transaction.Builder builder)
            throws InvalidInputException, IOException, GeneralSecurityException {
        int numInputs = (int) promptUserInt("Number of inputs");
        for (int i = 0; i < numInputs; i++) {
            System.out.println(String.format("[Input %d]", i));
            byte[] hash = ByteUtil.hexStringToByteArray(promptUser("Hash of input transaction")).
                    orElseThrow(() -> new InvalidInputException("Invalid SHA-256 hash"));
            int index = (int) promptUserInt("Index of input funds");
            String privateFilename = promptUser("Filename for private key");

            ShaTwoFiftySix sha256 = ShaTwoFiftySix.create(hash).
                    orElseThrow(() -> new InvalidInputException("Invalid SHA-256 hash"));

            builder.addInput(new TxIn(sha256, index), Crypto.loadPrivateKey(privateFilename));
        }
    }

    private void getOutputs(Transaction.Builder builder)
            throws InvalidInputException, IOException, GeneralSecurityException {
        int numOutputs = (int) promptUserInt("Number of outputs");
        for (int i = 0; i < numOutputs; i++) {
            System.out.println(String.format("[Output %d]", i));
            String publicKeyFilename = promptUser("Public key of recipient");
            long amount = promptUserInt("Amount to send to recipient");

            builder.addOutput(new TxOut(amount, Crypto.loadPublicKey(publicKeyFilename)));
        }
    }

    private long promptUserInt(String prompt) throws InvalidInputException, IOException {
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

    private static class InvalidInputException extends Exception {
        private InvalidInputException(String message) {
            super(message);
        }
    }
}
