import cli.ClientInterface;
import network.Miner;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Random;

public class Main {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No command specified");
            System.exit(1);
        }
        Crypto.init();
        switch (args[0]) {
            case "node":
                if (!runNode(args)) {
                    System.exit(1);
                };
                break;
            case "client":
                new ClientInterface().startInterface();
                break;
            default:
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                System.exit(1);
        }
    }

    public static boolean runNode(String[] args) throws GeneralSecurityException, IOException {
        if (args.length < 5) {
            System.err.println("usage: node <port> <public-key> <private-key> <privileged-key> (<ip-address>:<port>)*");
            return false;
        }
        int port = Integer.parseInt(args[1]);
        PublicKey myPublic = Crypto.loadPublicKey(args[2]);
        PrivateKey myPrivate = Crypto.loadPrivateKey(args[3]);
        PublicKey privilegedKey = Crypto.loadPublicKey(args[4]);

        Miner miner = new Miner(port, new KeyPair(myPublic, myPrivate), privilegedKey);
        for (int i = 5; i < args.length; i++) {
            String[] pieces = args[i].split(":");
            if (pieces.length != 2) {
                String msg = String.format("Invalid address %s", args[i]);
                System.err.println(msg);
                continue;
            }
            miner.connect(pieces[0], Integer.parseInt(pieces[1]));
        }
        miner.startMiner();
        return true;
    }

    public static void junkExampleTest() {
        // Just junk example of testing

        Miner miner = new Miner(4446, null, null); // TODO
        miner.startMiner();

        ArrayList<InetSocketAddress> hosts = new ArrayList<>();
        hosts.add(new InetSocketAddress("10.132.4.134", 4444));
//        hosts.add(new InetSocketAddress("10.132.7.187", 4445));
        miner.connectAll(hosts);

//        Transaction rTransaction = randomTransaction();
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        rTransaction.serializeWithSignatures(new DataOutputStream(outputStream));
//        Message message = new Message((byte)0, outputStream.toByteArray());
//
//        miner.broadcast(message);
//        miner.broadcast(message);
//        miner.broadcast(message);

    }

    protected static Transaction randomTransaction() throws GeneralSecurityException, IOException {
        KeyPair senderPair = Crypto.signatureKeyPair();
        KeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair.getPrivate())
                .addOutput(new TxOut(100, recipientPair.getPublic()))
                .build();
    }

    protected static byte[] randomBytes(int length) {

        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
