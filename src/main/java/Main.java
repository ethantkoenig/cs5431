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
    public static void main(String[] args) throws Exception {
        // Just junk example of testing

        Miner miner = new Miner(4444);
        miner.startMiner();

        Miner miner2 = new Miner(4445);
        ArrayList<InetSocketAddress> hosts = new ArrayList<>();
        hosts.add(new InetSocketAddress("localhost", 4444));
        miner2.connectAll(hosts);
        RTransaction rTransaction = randomTransaction();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        rTransaction.serializeWithSignatures(new DataOutputStream(outputStream));
        Message message = new Message((byte)0, outputStream.toByteArray());

        miner2.broadcast(message);
        miner2.broadcast(message);
        miner2.broadcast(message);

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
        new Random().nextBytes(bytes);
        return bytes;
    }
}
