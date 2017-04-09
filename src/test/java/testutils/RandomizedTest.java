package testutils;

import block.Block;
import block.UnspentTransactions;
import org.junit.Before;
import org.junit.BeforeClass;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Random;

/**
 * Shared boilerplate code for randomized tests
 */
public abstract class RandomizedTest {

    protected Random random;
    protected String errorMessage;

    @BeforeClass
    public static void setUpClass() {
        Crypto.init();
    }

    @Before
    public void setUp() throws Exception {
        SeededRandom seededRandom = SeededRandom.randomSeed();
        random = seededRandom.random();
        errorMessage = seededRandom.errorMessage();

        Config.setSecureRandom(new InsecureSecureRandom(random));
    }

    protected byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    protected String randomAsciiString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (32 + random.nextInt(127 - 32));
            builder.append(c);
        }
        return builder.toString();
    }

    protected Transaction randomTransaction() throws GeneralSecurityException, IOException {
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair.privateKey)
                .addOutput(new TxOut(100, recipientPair.publicKey))
                .build();
    }

    protected Block randomBlock(ShaTwoFiftySix previousHash) throws GeneralSecurityException, IOException {
        Block b = Block.empty(previousHash);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            b.addTransaction(randomTransaction());
        }
        b.addReward(Crypto.signatureKeyPair().publicKey);
        b.setRandomNonce(random);
        return b;
    }

    protected Pair<Block, UnspentTransactions> randomValidBlock(ShaTwoFiftySix previousHash)
            throws GeneralSecurityException, IOException {
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = Crypto.signatureKeyPair();

        TxOut output = new TxOut(1 + random.nextInt(1024), recipientPair.publicKey);
        Transaction initTransaction = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), senderPair.privateKey)
                .addOutput(output)
                .build();

        Block block = Block.empty(previousHash);

        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            senderPair = recipientPair;
            recipientPair = Crypto.signatureKeyPair();

            Transaction previous = i > 0 ? block.transactions[i - 1] : initTransaction;

            block.transactions[i] = new Transaction.Builder()
                    .addInput(
                            new TxIn(previous.getShaTwoFiftySix(), 0),
                            senderPair.privateKey
                    )
                    .addOutput(new TxOut(output.value, recipientPair.publicKey))
                    .build();
        }

        UnspentTransactions unspent = UnspentTransactions.empty();
        unspent.put(initTransaction.getShaTwoFiftySix(), 0, output);
        return new Pair<>(block, unspent);
    }

    protected ShaTwoFiftySix randomShaTwoFiftySix() {
        return ShaTwoFiftySix.create(randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES)).
                orElseThrow(() -> new AssertionError("Unable to generate random SHA-256"));
    }

    protected int[] randomPermutation(int n) {
        int[] array = new int[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        for (int j = n - 1; j > 0; j--) {
            int index = random.nextInt(j + 1);
            int tmp = array[j];
            array[j] = array[index];
            array[index] = tmp;
        }
        return array;
    }
}
