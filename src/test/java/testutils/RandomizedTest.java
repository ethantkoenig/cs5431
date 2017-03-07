package testutils;

import block.Block;
import block.UnspentTransactions;
import org.junit.Before;
import org.junit.BeforeClass;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
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
    public void setUp() {
        SeededRandom seededRandom = SeededRandom.randomSeed();
        random = seededRandom.random();
        errorMessage = seededRandom.errorMessage();
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

    protected RTransaction randomTransaction() throws GeneralSecurityException, IOException {
        KeyPair senderPair = Crypto.signatureKeyPair();
        KeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new RTransaction.Builder()
                .addInput(new RTxIn(hash, 0), senderPair.getPrivate())
                .addOutput(new RTxOut(100, recipientPair.getPublic()))
                .build();
    }

    protected Block randomBlock(ShaTwoFiftySix previousHash) throws GeneralSecurityException, IOException {
        Block b = Block.empty(previousHash);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            b.addTransaction(randomTransaction());
        }
        b.addReward(Crypto.signatureKeyPair().getPublic());
        b.setRandomNonce(random);
        return b;
    }

    protected Pair<Block, UnspentTransactions> randomValidBlock(ShaTwoFiftySix previousHash)
            throws GeneralSecurityException, IOException {
        KeyPair senderPair = Crypto.signatureKeyPair();
        KeyPair recipientPair = Crypto.signatureKeyPair();

        RTxOut output = new RTxOut(1 + random.nextInt(1024), recipientPair.getPublic());
        RTransaction initTransaction = new RTransaction.Builder()
                .addInput(new RTxIn(randomShaTwoFiftySix(), 0), senderPair.getPrivate())
                .addOutput(output)
                .build();

        Block block = Block.empty(randomShaTwoFiftySix());

        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            senderPair = recipientPair;
            recipientPair = Crypto.signatureKeyPair();

            RTransaction previous = i > 0 ? block.transactions[i-1] : initTransaction;

            block.transactions[i] = new RTransaction.Builder()
                    .addInput(
                            new RTxIn(previous.getShaTwoFiftySix(), 0),
                            senderPair.getPrivate()
                    )
                    .addOutput(new RTxOut(output.value, recipientPair.getPublic()))
                    .build();
        }

        UnspentTransactions unspent = UnspentTransactions.empty();
        unspent.put(initTransaction.getShaTwoFiftySix(), 0, output);
        return new Pair<>(block, unspent);
    }

    protected ShaTwoFiftySix randomShaTwoFiftySix() {
        return ShaTwoFiftySix.create(randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES));
    }
}
