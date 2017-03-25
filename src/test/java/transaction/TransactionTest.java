package transaction;

import block.UnspentTransactions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs a basic transaction between 2 peers, and signs and verifies (sig).
 */

public class TransactionTest extends RandomizedTest {

    @Test
    public void testToString() throws Exception {
        for (int i = 0; i < 5; ++i) {
            System.out.println(randomTransaction().toString());
        }
    }

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testEqualsHashCode() throws Exception {
        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        long value = random.nextInt(Integer.MAX_VALUE);
        Transaction tx1 = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.getPrivate())
                .addOutput(new TxOut(value, recipientPair.getPublic()))
                .build();

        Transaction tx2 = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.getPrivate())
                .addOutput(new TxOut(value, recipientPair.getPublic()))
                .build();

        Transaction anotherTx = randomTransaction();

        TestUtils.assertEqualsWithHashCode(errorMessage, tx1, tx1);
        TestUtils.assertEqualsWithHashCode(errorMessage, tx1, tx2);
        Assert.assertNotEquals(errorMessage, tx1, anotherTx);
        Assert.assertNotEquals(errorMessage, tx1, null);
    }

    @Test
    public void testRollback() throws Exception {
        KeyPair initialPair = randomKeyPair();
        KeyPair middlePair = randomKeyPair();
        KeyPair finalPair = randomKeyPair();

        TxOut middleOut0 = new TxOut(100, middlePair.getPublic());
        TxOut middleOut1 = new TxOut(200, middlePair.getPublic());
        Transaction transaction1 = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), initialPair.getPrivate())
                .addOutput(middleOut0)
                .addOutput(middleOut1)
                .build();

        TxOut finalOut0 = new TxOut(300, finalPair.getPublic());
        Transaction transaction2 = new Transaction.Builder()
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 0), middlePair.getPrivate())
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 1), middlePair.getPrivate())
                .addOutput(finalOut0)
                .build();

        Map<ShaTwoFiftySix, Transaction> transactions = new HashMap<>();
        transactions.put(transaction1.getShaTwoFiftySix(), transaction1);
        transactions.put(transaction2.getShaTwoFiftySix(), transaction2);
        TransactionLookup lookup = hash -> Optional.ofNullable(transactions.get(hash));

        UnspentTransactions unspentTransactions = UnspentTransactions.empty();
        unspentTransactions.put(transaction2.getShaTwoFiftySix(), 0, finalOut0);
        Assert.assertTrue(errorMessage, transaction2.rollback(unspentTransactions, lookup));

        Assert.assertTrue(unspentTransactions.contains(transaction1.getShaTwoFiftySix(), 0));
        Assert.assertTrue(unspentTransactions.contains(transaction1.getShaTwoFiftySix(), 1));
        Assert.assertFalse(unspentTransactions.contains(transaction2.getShaTwoFiftySix(), 0));

        Assert.assertFalse(errorMessage, transaction2.rollback(unspentTransactions, lookup));
        Assert.assertFalse(errorMessage, transaction1.rollback(unspentTransactions, lookup));
    }

    @Test
    public void doTransaction() throws Exception {
        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.getPrivate())
                .addOutput(new TxOut(100, recipientPair.getPublic()))
                .build();

        Assert.assertTrue(errorMessage,
                tx.verifySignature(0, senderPair.getPublic()));

        byte[] serialized = ByteUtil.asByteArray(tx::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(tx.equals(deserialized));
        Assert.assertTrue(deserialized.verifySignature(0, senderPair.getPublic()));
    }

    //  Sign transaction with key that does not match, should fail.
    @Test
    public void failTransaction() throws Exception {
        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 1), senderPair.getPrivate())
                .addOutput(new TxOut(1000, recipientPair.getPublic()))
                .build();

        ShaTwoFiftySix hash2 = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash2, 2), senderPair.getPrivate())
                .addOutput(new TxOut(100, recipientPair.getPublic()))
                .build();

        Assert.assertFalse(errorMessage, tx.equals(txn));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, recipientPair.getPublic()));
    }

    @Test
    public void doThreeOutTransaction() throws Exception {
        KeyPair senderPair = randomKeyPair();

        KeyPair recipientPair1 = randomKeyPair();
        KeyPair recipientPair2 = randomKeyPair();
        KeyPair recipientPair3 = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 4), senderPair.getPrivate())
                .addOutput(new TxOut(100, recipientPair1.getPublic()))
                .addOutput(new TxOut(200, recipientPair2.getPublic()))
                .addOutput(new TxOut(300, recipientPair3.getPublic()))
                .build();

        byte[] serialized = ByteUtil.asByteArray(txn::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(txn.equals(deserialized));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(0, senderPair.getPublic()));
    }

    @Test
    public void doMultiInOutTransaction() throws Exception {
        KeyPair senderPair1 = randomKeyPair();
        KeyPair senderPair2 = randomKeyPair();
        KeyPair senderPair3 = randomKeyPair();

        KeyPair recipientPair1 = randomKeyPair();
        KeyPair recipientPair2 = randomKeyPair();
        KeyPair recipientPair3 = randomKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair1.getPrivate())
                .addInput(new TxIn(hash, 2), senderPair2.getPrivate())
                .addInput(new TxIn(hash, 5), senderPair3.getPrivate())
                .addOutput(new TxOut(100, recipientPair1.getPublic()))
                .addOutput(new TxOut(200, recipientPair2.getPublic()))
                .addOutput(new TxOut(300, recipientPair3.getPublic()))
                .build();

        byte[] serialized = ByteUtil.asByteArray(txn::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(txn.equals(deserialized));

        Assert.assertTrue(errorMessage,
                txn.verifySignature(0, senderPair1.getPublic()));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, senderPair2.getPublic()));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, senderPair3.getPublic()));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(1, senderPair1.getPublic()));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(1, senderPair2.getPublic()));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(1, senderPair3.getPublic()));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(2, senderPair1.getPublic()));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(2, senderPair2.getPublic()));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(2, senderPair3.getPublic()));
    }

    @Test
    public void testVerificationFailure() throws Exception {
        KeyPair badSpender = randomKeyPair();
        TxOut unspent = new TxOut(50, badSpender.getPublic());
        ShaTwoFiftySix hash = randomShaTwoFiftySix();

        UnspentTransactions unspentTxs = UnspentTransactions.empty();
        unspentTxs.put(hash, 0, unspent);

        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), randomKeyPair().getPrivate())
                .addOutput(new TxOut(50, badSpender.getPublic()))
                .build();

        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.getPrivate())
                .addOutput(new TxOut(49, badSpender.getPublic()))
                .build();

        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.getPrivate())
                .addOutput(new TxOut(70, badSpender.getPublic()))
                .addOutput(new TxOut(-20, badSpender.getPublic()))
                .build();
        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.getPrivate())
                .addOutput(new TxOut(1L << 62, badSpender.getPublic()))
                .addOutput(new TxOut(1L << 62, badSpender.getPublic()))
                .addOutput(new TxOut(1L << 62, badSpender.getPublic()))
                .addOutput(new TxOut(1L << 62, badSpender.getPublic()))
                .addOutput(new TxOut(50, badSpender.getPublic()))
                .build();
        Assert.assertFalse(tx.verify(unspentTxs.copy()));
    }

    @Test
    public void testGetterFailure() throws Exception {
        Transaction tx = randomTransaction();

        TestUtils.assertThrows(errorMessage, () -> tx.getInput(-1), IllegalArgumentException.class);
        TestUtils.assertThrows(errorMessage, () -> tx.getInput(tx.numInputs), IllegalArgumentException.class);
        TestUtils.assertThrows(errorMessage, () -> tx.getOutput(-1), IllegalArgumentException.class);
        TestUtils.assertThrows(errorMessage, () -> tx.getOutput(tx.numOutputs), IllegalArgumentException.class);
    }
}
