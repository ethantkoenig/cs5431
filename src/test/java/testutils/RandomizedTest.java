package testutils;

import block.MiningBlock;
import block.UnspentTransactions;
import org.junit.Before;
import org.junit.BeforeClass;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Random;

/**
 * Shared boilerplate code for randomized tests
 */
public abstract class RandomizedTest {

    protected Random random;
    private SecureRandom secureRandom;
    private KeyPairGenerator generator;
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

        generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
        secureRandom = new InsecureSecureRandom(random);
        generator.initialize(ecSpec, secureRandom);
    }

    protected KeyPair randomKeyPair() throws GeneralSecurityException {
        KeyPair pair = generator.generateKeyPair();
        return pair;
    }

    protected long nonNegativeLong() {
        long l = random.nextLong();
        if (l < 0) {
            l = ~l;
        }
        return l;
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
        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair.getPrivate())
                .addOutput(new TxOut(100, recipientPair.getPublic()))
                .build();
    }

    protected MiningBlock randomBlock(ShaTwoFiftySix previousHash) throws GeneralSecurityException, IOException {
        MiningBlock b = MiningBlock.empty(previousHash);
        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            b.addTransaction(randomTransaction());
        }
        b.addReward(randomKeyPair().getPublic());
        b.setRandomNonce(random);
        return b;
    }

    protected Pair<MiningBlock, UnspentTransactions> randomValidBlock(ShaTwoFiftySix previousHash)
            throws GeneralSecurityException, IOException {
        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        TxOut output = new TxOut(1 + random.nextInt(1024), recipientPair.getPublic());
        Transaction initTransaction = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), senderPair.getPrivate())
                .addOutput(output)
                .build();

        MiningBlock block = MiningBlock.empty(previousHash);

        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            senderPair = recipientPair;
            recipientPair = randomKeyPair();

            Transaction previous = i > 0 ? block.transactions[i - 1] : initTransaction;

            block.transactions[i] = new Transaction.Builder()
                    .addInput(
                            new TxIn(previous.getShaTwoFiftySix(), 0),
                            senderPair.getPrivate()
                    )
                    .addOutput(new TxOut(output.value, recipientPair.getPublic()))
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
