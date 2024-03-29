package testutils;

import block.Block;
import block.UnspentTransactions;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import org.junit.Before;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared boilerplate code for randomized tests
 */
public abstract class RandomizedTest {

    protected Random random;
    protected String errorMessage;
    protected Crypto crypto;

    @Before
    public void setUp() throws Exception {
        SeededRandom seededRandom = SeededRandom.randomSeed();
        random = seededRandom.random();
        errorMessage = seededRandom.errorMessage();
        crypto = new Crypto(new InsecureSecureRandom(random));
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
        ECDSAKeyPair senderPair = crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair.privateKey)
                .addOutput(new TxOut(100, recipientPair.publicKey))
                .build();
    }

    protected Block randomBlock(ShaTwoFiftySix previousHash) throws GeneralSecurityException, IOException {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            transactions.add(randomTransaction());
        }
        Block b = Block.block(previousHash, transactions, crypto.signatureKeyPair().publicKey);
        b.setRandomNonce(random);
        return b;
    }

    protected Pair<Block, UnspentTransactions> randomValidBlock(ShaTwoFiftySix previousHash)
            throws GeneralSecurityException, IOException {
        ECDSAKeyPair senderPair = crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = crypto.signatureKeyPair();

        TxOut output = new TxOut(1 + random.nextInt(1024), recipientPair.publicKey);
        Transaction initTransaction = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), senderPair.privateKey)
                .addOutput(output)
                .build();

        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            senderPair = recipientPair;
            recipientPair = crypto.signatureKeyPair();

            Transaction previous = i > 0 ? transactions.get(i - 1) : initTransaction;

            transactions.add(new Transaction.Builder()
                    .addInput(
                            new TxIn(previous.getShaTwoFiftySix(), 0),
                            senderPair.privateKey
                    )
                    .addOutput(new TxOut(output.value, recipientPair.publicKey))
                    .build()
            );
        }

        Block block = Block.block(previousHash, transactions, senderPair.publicKey);

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
