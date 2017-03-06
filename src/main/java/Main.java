import network.Message;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Random;

public class Main {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        // Just junk example of testing

        Miner miner = new Miner(4446);
        miner.startMiner();

        ArrayList<InetSocketAddress> hosts = new ArrayList<>();
        hosts.add(new InetSocketAddress("10.132.4.134", 4444));
//        hosts.add(new InetSocketAddress("10.132.7.187", 4445));
        miner.connectAll(hosts);

        RTransaction rTransaction = randomTransaction();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        rTransaction.serializeWithSignatures(new DataOutputStream(outputStream));
        Message message = new Message((byte)0, outputStream.toByteArray());

        miner.broadcast(message);
        miner.broadcast(message);
        miner.broadcast(message);

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
