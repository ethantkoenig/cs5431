package block;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestModule;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Config;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class BlockTest extends RandomizedTest {
    private final Crypto crypto;

    public BlockTest() {
        Injector injector = Guice.createInjector(new TestModule());
        crypto = injector.getInstance(Crypto.class);
    }

    @Test
    public void testEquals() throws Exception {
        Block b1 = randomBlock(ShaTwoFiftySix.zero());

        Assert.assertNotEquals(errorMessage, b1, new Object());

        Block b2 = Block.block(ShaTwoFiftySix.zero(), b1.transactions, b1.reward.ownerPubKey);
        System.arraycopy(b1.nonce, 0, b2.nonce, 0, Block.NONCE_SIZE_IN_BYTES);

        TestUtils.assertEqualsWithHashCode(errorMessage, b1, b2);

        b2.nonceAddOne();

        Assert.assertNotEquals(errorMessage, b1, b2);
    }

    @Test
    public void testSerialize() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block = randomBlock(previousBlockHash);
        Block deserialized = Block.DESERIALIZER.deserialize(ByteUtil.asByteArray(block::serialize));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testSerializeGenesis() throws Exception {
        Block block = Block.genesis(crypto.signatureKeyPair().publicKey);
        Block deserialized = Block.DESERIALIZER.deserialize(ByteUtil.asByteArray(block::serialize));

        TestUtils.assertEqualsWithHashCode(errorMessage, block, deserialized);
        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testVerify() throws Exception {
        Config.setHashGoal(1);
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Pair<Block, UnspentTransactions> pair = randomValidBlock(previousBlockHash);
        Block block = pair.getLeft();
        block.findValidNonce();

        UnspentTransactions result = TestUtils.assertPresent(block.verifyNonGenesis(pair.getRight()));

        UnspentTransactions expected = UnspentTransactions.empty();
        Transaction lastTxn = block.transactions[Block.NUM_TRANSACTIONS_PER_BLOCK - 1];
        // TODO currently assumes that last transaction will only have one output
        expected.put(lastTxn.getShaTwoFiftySix(), 0, lastTxn.getOutput(0));
        expected.put(block.getShaTwoFiftySix(), 0, block.reward);
        TestUtils.assertEqualsWithHashCode(errorMessage, result, expected);

        block = randomBlock(randomShaTwoFiftySix());
        Assert.assertFalse(errorMessage, block.verifyNonGenesis(UnspentTransactions.empty()).isPresent());
    }

    @Test
    public void testVerifyGenesis() throws Exception {
        ECDSAKeyPair pair = crypto.signatureKeyPair();
        Assert.assertFalse(errorMessage,
                randomBlock(randomShaTwoFiftySix()).verifyGenesis(pair.publicKey));

        Block genesis = Block.genesis(pair.publicKey);
        genesis.findValidNonce();
        Assert.assertTrue(errorMessage, genesis.verifyGenesis(pair.publicKey));

        Block badGenesis = new Block(
                ShaTwoFiftySix.zero(),
                new Transaction[0],
                new TxOut(Block.REWARD_AMOUNT + 1, pair.publicKey)
        );
        Assert.assertFalse(errorMessage, badGenesis.verifyGenesis(pair.publicKey));
    }

    @Test
    public void testGetTransactionDifferences() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));

        Transaction txn1 = randomTransaction();
        Transaction txn2 = randomTransaction();
        Transaction txn3 = randomTransaction();

        ECDSAPublicKey publicKey = crypto.signatureKeyPair().publicKey;
        Block block1 = Block.block(previousBlockHash, Arrays.asList(txn1, txn2), publicKey);
        Block block2 = Block.block(previousBlockHash, Arrays.asList(txn2, txn3), publicKey);

        Assert.assertEquals(errorMessage,
                block1.getTransactionDifferences(block2),
                Collections.singletonList(txn3));
        Assert.assertEquals(errorMessage,
                block2.getTransactionDifferences(block1),
                Collections.singletonList(txn1));
    }

    @Test
    public void testCheckHash() throws Exception {
        Config.setHashGoal(1);
        Block block = randomBlock(randomShaTwoFiftySix());
        for (int i = 0; i < 1000; i++) {
            block.nonceAddOne();
            if (block.checkHash()) {
                ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(block::serialize));
                Assert.assertTrue(hash.checkHashZeros(1));
            }
        }
    }
}
