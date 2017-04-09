package transaction;

import block.UnspentTransactions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.Crypto;
import utils.ECDSAKeyPair;
import utils.ShaTwoFiftySix;

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
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        long value = random.nextInt(Integer.MAX_VALUE);
        Transaction tx1 = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.privateKey)
                .addOutput(new TxOut(value, recipientPair.publicKey))
                .build();

        Transaction tx2 = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.privateKey)
                .addOutput(new TxOut(value, recipientPair.publicKey))
                .build();

        Transaction anotherTx = randomTransaction();

        TestUtils.assertEqualsWithHashCode(errorMessage, tx1, tx1);
        TestUtils.assertEqualsWithHashCode(errorMessage, tx1, tx2);
        Assert.assertNotEquals(errorMessage, tx1, anotherTx);
        Assert.assertNotEquals(errorMessage, tx1, null);
    }

    @Test
    public void testRollback() throws Exception {
        ECDSAKeyPair initialPair = Crypto.signatureKeyPair();
        ECDSAKeyPair middlePair = Crypto.signatureKeyPair();
        ECDSAKeyPair finalPair = Crypto.signatureKeyPair();

        TxOut middleOut0 = new TxOut(100, middlePair.publicKey);
        TxOut middleOut1 = new TxOut(200, middlePair.publicKey);
        Transaction transaction1 = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), initialPair.privateKey)
                .addOutput(middleOut0)
                .addOutput(middleOut1)
                .build();

        TxOut finalOut0 = new TxOut(300, finalPair.publicKey);
        Transaction transaction2 = new Transaction.Builder()
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 0), middlePair.privateKey)
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 1), middlePair.privateKey)
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
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 2), senderPair.privateKey)
                .addOutput(new TxOut(100, recipientPair.publicKey))
                .build();

        Assert.assertTrue(errorMessage,
                tx.verifySignature(0, senderPair.publicKey));

        byte[] serialized = ByteUtil.asByteArray(tx::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(tx.equals(deserialized));
        Assert.assertTrue(deserialized.verifySignature(0, senderPair.publicKey));
    }

    //  Sign transaction with key that does not match, should fail.
    @Test
    public void failTransaction() throws Exception {
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 1), senderPair.privateKey)
                .addOutput(new TxOut(1000, recipientPair.publicKey))
                .build();

        ShaTwoFiftySix hash2 = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash2, 2), senderPair.privateKey)
                .addOutput(new TxOut(100, recipientPair.publicKey))
                .build();

        Assert.assertFalse(errorMessage, tx.equals(txn));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, recipientPair.publicKey));
    }

    @Test
    public void doThreeOutTransaction() throws Exception {
        ECDSAKeyPair senderPair = Crypto.signatureKeyPair();

        ECDSAKeyPair recipientPair1 = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair2 = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair3 = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 4), senderPair.privateKey)
                .addOutput(new TxOut(100, recipientPair1.publicKey))
                .addOutput(new TxOut(200, recipientPair2.publicKey))
                .addOutput(new TxOut(300, recipientPair3.publicKey))
                .build();

        byte[] serialized = ByteUtil.asByteArray(txn::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(txn.equals(deserialized));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(0, senderPair.publicKey));
    }

    @Test
    public void doMultiInOutTransaction() throws Exception {
        ECDSAKeyPair senderPair1 = Crypto.signatureKeyPair();
        ECDSAKeyPair senderPair2 = Crypto.signatureKeyPair();
        ECDSAKeyPair senderPair3 = Crypto.signatureKeyPair();

        ECDSAKeyPair recipientPair1 = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair2 = Crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair3 = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Transaction txn = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), senderPair1.privateKey)
                .addInput(new TxIn(hash, 2), senderPair2.privateKey)
                .addInput(new TxIn(hash, 5), senderPair3.privateKey)
                .addOutput(new TxOut(100, recipientPair1.publicKey))
                .addOutput(new TxOut(200, recipientPair2.publicKey))
                .addOutput(new TxOut(300, recipientPair3.publicKey))
                .build();

        byte[] serialized = ByteUtil.asByteArray(txn::serialize);
        Transaction deserialized = Transaction.DESERIALIZER.deserialize(serialized);

        Assert.assertTrue(txn.equals(deserialized));

        Assert.assertTrue(errorMessage,
                txn.verifySignature(0, senderPair1.publicKey));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, senderPair2.publicKey));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(0, senderPair3.publicKey));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(1, senderPair1.publicKey));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(1, senderPair2.publicKey));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(1, senderPair3.publicKey));

        Assert.assertFalse(errorMessage,
                txn.verifySignature(2, senderPair1.publicKey));
        Assert.assertFalse(errorMessage,
                txn.verifySignature(2, senderPair2.publicKey));
        Assert.assertTrue(errorMessage,
                txn.verifySignature(2, senderPair3.publicKey));
    }

    @Test
    public void testVerificationFailure() throws Exception {
        ECDSAKeyPair badSpender = Crypto.signatureKeyPair();
        TxOut unspent = new TxOut(50, badSpender.publicKey);
        ShaTwoFiftySix hash = randomShaTwoFiftySix();

        UnspentTransactions unspentTxs = UnspentTransactions.empty();
        unspentTxs.put(hash, 0, unspent);

        Transaction tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), Crypto.signatureKeyPair().privateKey)
                .addOutput(new TxOut(50, badSpender.publicKey))
                .build();

        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.privateKey)
                .addOutput(new TxOut(49, badSpender.publicKey))
                .build();

        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.privateKey)
                .addOutput(new TxOut(70, badSpender.publicKey))
                .addOutput(new TxOut(-20, badSpender.publicKey))
                .build();
        Assert.assertFalse(tx.verify(unspentTxs.copy()));

        tx = new Transaction.Builder()
                .addInput(new TxIn(hash, 0), badSpender.privateKey)
                .addOutput(new TxOut(1L << 62, badSpender.publicKey))
                .addOutput(new TxOut(1L << 62, badSpender.publicKey))
                .addOutput(new TxOut(1L << 62, badSpender.publicKey))
                .addOutput(new TxOut(1L << 62, badSpender.publicKey))
                .addOutput(new TxOut(50, badSpender.publicKey))
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
