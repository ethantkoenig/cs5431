import cli.ClientInterface;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
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
                // TODO eventually replace with actual code
                junkExampleTest();
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

    public static void junkExampleTest() {
        // Just junk example of testing

        Miner miner = new Miner(4446);
        miner.startMiner();

        ArrayList<InetSocketAddress> hosts = new ArrayList<>();
        hosts.add(new InetSocketAddress("10.132.4.134", 4444));
//        hosts.add(new InetSocketAddress("10.132.7.187", 4445));
        miner.connectAll(hosts);

//        RTransaction rTransaction = randomTransaction();
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        rTransaction.serializeWithSignatures(new DataOutputStream(outputStream));
//        Message message = new Message((byte)0, outputStream.toByteArray());
//
//        miner.broadcast(message);
//        miner.broadcast(message);
//        miner.broadcast(message);

    }

    protected static RTransaction randomTransaction() throws GeneralSecurityException, IOException {
        KeyPair senderPair = Crypto.signatureKeyPair();
        KeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new RTransaction.Builder()
                .addInput(new RTxIn(hash, 0), senderPair.getPrivate())
                .addOutput(new RTxOut(100, recipientPair.getPublic()))
                .build();
    }

    protected static byte[] randomBytes(int length) {

        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
