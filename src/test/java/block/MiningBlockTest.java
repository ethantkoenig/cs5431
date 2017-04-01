package block;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxOut;
import utils.*;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;

public class MiningBlockTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testEquals() throws Exception {
        MiningBlock b1 = randomBlock(ShaTwoFiftySix.zero());

        Assert.assertNotEquals(errorMessage, b1, new Object());

        MiningBlock b2 = MiningBlock.empty(ShaTwoFiftySix.zero());
        for (Transaction tx : b1) {
            b2.addTransaction(tx);
        }
        b2.addReward(b1.reward.ownerPubKey);
        for (int i = 0; i < MiningBlock.NONCE_SIZE_IN_BYTES; ++i) {
            b2.nonce[i] = b1.nonce[i];
        }

        TestUtils.assertEqualsWithHashCode(errorMessage, b1, b2);

        b2.nonceAddOne();

        Assert.assertNotEquals(errorMessage, b1, b2);
    }

    @Test
    public void testSerialize() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        MiningBlock block = MiningBlock.empty(previousBlockHash);
        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = randomTransaction();
        }
        block.addReward(randomKeyPair().getPublic());

        MiningBlock deserialized = MiningBlock.DESERIALIZER.deserialize(ByteUtil.asByteArray(block::serialize));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testAddReward() throws Exception {
        MiningBlock b = MiningBlock.empty(ShaTwoFiftySix.zero());
        PublicKey key = randomKeyPair().getPublic();
        b.addReward(key);

        Assert.assertEquals(MiningBlock.REWARD_AMOUNT, b.reward.value);
        Assert.assertEquals(key, b.reward.ownerPubKey);

        TestUtils.assertThrows(errorMessage, () -> b.addReward(key), IllegalStateException.class);
    }

    @Test
    public void testAddTransaction() throws Exception {
        MiningBlock b = MiningBlock.empty(ShaTwoFiftySix.zero());

        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
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
        MiningBlock block = MiningBlock.genesis();
        block.addReward(randomKeyPair().getPublic());

        MiningBlock deserialized = MiningBlock.DESERIALIZER.deserialize(ByteUtil.asByteArray(block::serialize));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testVerify() throws Exception {
        Config.HASH_GOAL.set(1);
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Pair<MiningBlock, UnspentTransactions> pair = randomValidBlock(previousBlockHash);
        MiningBlock block = pair.getLeft();
        block.addReward(randomKeyPair().getPublic());
        while (!block.checkHash()) { // mine the block
            block.nonceAddOne();
        }

        UnspentTransactions result = TestUtils.assertPresent(block.verify(pair.getRight()));

        UnspentTransactions expected = UnspentTransactions.empty();
        Transaction lastTxn = block.transactions[MiningBlock.NUM_TRANSACTIONS_PER_BLOCK - 1];
        // TODO currently assumes that last transaction will only have one output
        expected.put(lastTxn.getShaTwoFiftySix(), 0, lastTxn.getOutput(0));
        expected.put(block.getShaTwoFiftySix(), 0, block.reward);
        TestUtils.assertEqualsWithHashCode(errorMessage, result, expected);

        block.reward = new TxOut(block.reward.value + 1, block.reward.ownerPubKey);
        Assert.assertFalse(errorMessage, block.verify(pair.getRight()).isPresent());

        block = randomBlock(randomShaTwoFiftySix());
        Assert.assertFalse(errorMessage, block.verify(UnspentTransactions.empty()).isPresent());
    }

    @Test
    public void testVerifyGenesis() throws Exception {
        KeyPair pair = randomKeyPair();
        Assert.assertFalse(errorMessage,
                randomBlock(randomShaTwoFiftySix()).verifyGenesis(pair.getPublic()));

        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(pair.getPublic());
        Assert.assertTrue(errorMessage, genesis.verifyGenesis(pair.getPublic()));

        genesis.reward = new TxOut(MiningBlock.REWARD_AMOUNT + 1, pair.getPublic());
        Assert.assertFalse(errorMessage, genesis.verifyGenesis(pair.getPublic()));
    }

    @Test
    public void testGetTransactionDifferences() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        MiningBlock block1 = MiningBlock.empty(previousBlockHash);
        MiningBlock block2 = MiningBlock.empty(previousBlockHash);

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
        Config.HASH_GOAL.set(1);
        MiningBlock block = randomBlock(randomShaTwoFiftySix());
        for (int i = 0; i < 1000; i++) {
            block.nonceAddOne();
            if (block.checkHash()) {
                ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(block::serialize));
                Assert.assertTrue(hash.checkHashZeros(1));
            }
        }
    }

    @Test
    public void testSerializeFailures() throws Exception {
        MiningBlock block = MiningBlock.empty(randomShaTwoFiftySix());

        TestUtils.assertThrows(errorMessage, () -> block.getShaTwoFiftySix(), IllegalStateException.class);

        while (!block.isFull()) {
            block.addTransaction(randomTransaction());
        }

        TestUtils.assertThrows(errorMessage, () -> block.getShaTwoFiftySix(), IllegalStateException.class);
    }
}
