package block;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.RTransaction;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;

public class BlockTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
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

        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testSerializeGenesis() throws Exception {
        Block block = Block.genesis();
        block.addReward(Crypto.signatureKeyPair().getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

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
        RTransaction lastTxn = block.transactions[Block.NUM_TRANSACTIONS_PER_BLOCK - 1];
        // TODO currently assumes that last transaction will only have one output
        expected.put(lastTxn.getShaTwoFiftySix(), 0, lastTxn.getOutput(0));
        Assert.assertEquals(errorMessage, result.get(), expected);
    }

    @Test
    public void testGetTransactionDifferences() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block1 = Block.empty(previousBlockHash);
        Block block2 = Block.empty(previousBlockHash);

        RTransaction txn1 = randomTransaction();
        RTransaction txn2 = randomTransaction();
        RTransaction txn3 = randomTransaction();

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
}
