package block;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxOut;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Optional;

public class BlockTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testEquals() throws Exception {
        Block b1 = randomBlock(ShaTwoFiftySix.zero());

        Assert.assertFalse(errorMessage, b1.equals(new Object()));

        Block b2 = Block.empty(ShaTwoFiftySix.zero());
        for (Transaction tx: b1) {
            b2.addTransaction(tx);
        }
        b2.addReward(b1.reward.ownerPubKey);
        for (int i = 0; i < Block.NONCE_SIZE_IN_BYTES; ++i) {
            b2.nonce[i] = b1.nonce[i];
        }

        Assert.assertTrue(errorMessage, b1.equals(b2));

        b2.nonceAddOne();

        Assert.assertFalse(errorMessage, b1.equals(b2));
    }

    @Test
    public void testSerialize() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block = Block.empty(previousBlockHash);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = randomTransaction();
        }
        block.addReward(Crypto.signatureKeyPair().getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testAddReward() throws Exception {
        Block b = Block.empty(ShaTwoFiftySix.zero());
        PublicKey key = Crypto.signatureKeyPair().getPublic();
        b.addReward(key);

        Assert.assertEquals(Block.REWARD_AMOUNT, b.reward.value);
        Assert.assertEquals(key, b.reward.ownerPubKey);

        try {
            b.addReward(key);
            // Should have thrown
            Assert.fail(errorMessage);
        } catch (IllegalStateException e) {
            // We should be here
        }
    }

    @Test
    public void testAddTransaction() throws Exception {
        Block b = Block.empty(ShaTwoFiftySix.zero());

        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            Assert.assertFalse(errorMessage, b.isFull());
            Transaction tx = randomTransaction();
            b.addTransaction(tx);
            Assert.assertEquals(tx, b.transactions[i]);
        }

        Assert.assertTrue(errorMessage, b.isFull());
        Assert.assertFalse(errorMessage, b.addTransaction(randomTransaction()));
    }

    @Test
    public void testSerializeGenesis() throws Exception {
        Block block = Block.genesis();
        block.addReward(Crypto.signatureKeyPair().getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testVerify() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Pair<Block, UnspentTransactions> pair = randomValidBlock(previousBlockHash);
        Block block = pair.getLeft();
        block.addReward(Crypto.signatureKeyPair().getPublic());

        Optional<UnspentTransactions> result = block.verify(pair.getRight());
        Assert.assertTrue(errorMessage, result.isPresent());

        UnspentTransactions expected = UnspentTransactions.empty();
        Transaction lastTxn = block.transactions[Block.NUM_TRANSACTIONS_PER_BLOCK - 1];
        // TODO currently assumes that last transaction will only have one output
        expected.put(lastTxn.getShaTwoFiftySix(), 0, lastTxn.getOutput(0));
        TestUtils.assertEqualsWithHashCode(errorMessage, result.get(), expected);

        block.reward = new TxOut(block.reward.value + 1, block.reward.ownerPubKey);
        result = block.verify(pair.getRight());
        Assert.assertFalse(errorMessage, result.isPresent());

        block = randomBlock(randomShaTwoFiftySix());
        result = block.verify(UnspentTransactions.empty());
        Assert.assertFalse(errorMessage, result.isPresent());
    }

    @Test
    public void testVerifyGenesis() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        Assert.assertFalse(errorMessage,
                randomBlock(randomShaTwoFiftySix()).verifyGenesis(pair.getPublic()));

        Block genesis = Block.genesis();
        genesis.addReward(pair.getPublic());
        Assert.assertTrue(errorMessage, genesis.verifyGenesis(pair.getPublic()));

        genesis.reward = new TxOut(Block.REWARD_AMOUNT + 1, pair.getPublic());
        Assert.assertFalse(errorMessage, genesis.verifyGenesis(pair.getPublic()));
    }

    @Test
    public void testGetTransactionDifferences() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block1 = Block.empty(previousBlockHash);
        Block block2 = Block.empty(previousBlockHash);

        Transaction txn1 = randomTransaction();
        Transaction txn2 = randomTransaction();
        Transaction txn3 = randomTransaction();

        block1.addTransaction(txn1);
        block1.addTransaction(txn2);
        block2.addTransaction(txn2);
        block2.addTransaction(txn3);

        Assert.assertEquals(errorMessage,
                block1.getTransactionDifferences(block2),
                Collections.singletonList(txn3));
        Assert.assertEquals(errorMessage,
                block2.getTransactionDifferences(block1),
                Collections.singletonList(txn1));
    }

    @Test
    public void testCheckHash() throws Exception {
        Block block = randomBlock(randomShaTwoFiftySix());
        for (int i = 0; i < 1000; i++) {
            block.nonceAddOne();
            if (block.checkHashWith(1)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                block.serialize(new DataOutputStream(outputStream));
                ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(outputStream.toByteArray());
                Assert.assertTrue(hash.checkHashZeros(1));
            }
        }
    }

    @Test
    public void testSerializeFailures() throws Exception {
        Block block = Block.empty(randomShaTwoFiftySix());

        try {
            block.getShaTwoFiftySix();
            // should throw
            Assert.fail(errorMessage);
        } catch (IllegalStateException e) {
            // should reach
        }

        while (!block.isFull()) {
            block.addTransaction(randomTransaction());
        }

        try {
            block.getShaTwoFiftySix();
            // should throw
            Assert.fail(errorMessage);
        } catch (IllegalStateException e) {
            // should reach
        }
    }
}
