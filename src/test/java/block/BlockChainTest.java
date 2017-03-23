package block;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.*;

import static org.junit.Assert.*;

/**
 * Created by eperdew on 3/5/17.
 */
public class BlockChainTest extends RandomizedTest {

    @Test
    public void testVerify() throws Exception {
        // Test getUnspentTransactionsAt(...) and verifyBlock(...)
        BlockChain bc = new BlockChain();
        assertEquals(UnspentTransactions.empty(),
                bc.getUnspentTransactionsAt(randomBlock(randomShaTwoFiftySix())));

        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        Block genesis = Block.genesis();
        genesis.addReward(senderPair.getPublic());
        bc.insertBlock(genesis);

        Assert.assertTrue(errorMessage, bc.verifyBlock(genesis).isPresent());
        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        Block next = Block.empty(genesis.getShaTwoFiftySix());
        next.addReward(senderPair.getPublic());

        Pair<ShaTwoFiftySix,Integer> prevTxOut = new Pair<>(genesis.getShaTwoFiftySix(),0);

        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            Transaction tx = new Transaction.Builder()
                    .addInput(new TxIn(prevTxOut.getLeft(),0), senderPair.getPrivate())
                    .addOutput(new TxOut(Block.REWARD_AMOUNT - (i + 1), senderPair.getPublic()))
                    .addOutput(new TxOut(1, recipientPair.getPublic()))
                    .build();
            next.addTransaction(tx);
            unspentTxs.put(tx.getShaTwoFiftySix(), 1, tx.getOutput(1));
            prevTxOut = new Pair<>(tx.getShaTwoFiftySix(),i);
        }

        unspentTxs.put(prevTxOut.getLeft(), 0,
                next.transactions[Block.NUM_TRANSACTIONS_PER_BLOCK-1].getOutput(0));
        unspentTxs.put(next.getShaTwoFiftySix(), 0, next.reward);

        Assert.assertTrue(errorMessage, bc.verifyBlock(next).isPresent());
        bc.insertBlock(next);
        Assert.assertTrue(errorMessage, bc.verifyBlock(next).isPresent());
        assertEquals(errorMessage, unspentTxs, bc.getUnspentTransactionsAt(next));

        Assert.assertFalse(errorMessage, bc.verifyBlock(randomBlock(randomShaTwoFiftySix())).isPresent());

        Block fauxGenesis = Block.genesis();
        fauxGenesis.reward = new TxOut(Block.REWARD_AMOUNT + 1, Crypto.signatureKeyPair().getPublic());
        Assert.assertFalse(errorMessage, bc.verifyBlock(fauxGenesis).isPresent());
    }

    @Test
    public void getBlockWithHash() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        assertEquals(Optional.of(genesis), bc.getBlockWithHash(genesis.getShaTwoFiftySix()));

        Block prev = genesis;

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            assertEquals(Optional.of(next), bc.getBlockWithHash(next.getShaTwoFiftySix()));
            prev = next;
        }
    }

    @Test
    public void insertBlock() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        ShaTwoFiftySix randomHash = randomShaTwoFiftySix();
        Block b = randomBlock(randomHash);

        Assert.assertFalse(errorMessage, bc.insertBlock(b));
        assertEquals(errorMessage, Optional.empty(), bc.getBlockWithHash(b.getShaTwoFiftySix()));
    }

    @Test
    public void insertBlockEmpty() throws Exception {
        BlockChain bc = new BlockChain();
        assertFalse(errorMessage, bc.insertBlock(randomBlock(randomShaTwoFiftySix())));

        Block genesis = Block.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        assertTrue(errorMessage, bc.insertBlock(genesis));
        assertTrue(errorMessage, bc.containsBlock(genesis));
        assertEquals(errorMessage, genesis, bc.getCurrentHead());
    }

    @Test
    public void insertDuplicateGenesis() throws Exception {
        BlockChain bc = new BlockChain();
        PublicKey key1 = randomKeyPair().getPublic();
        PublicKey key2 = randomKeyPair().getPublic();

        Block genesis1 = Block.genesis();
        genesis1.addReward(key1);

        Block genesis2 = Block.genesis();
        genesis2.addReward(key2);

        bc.insertBlock(genesis1);

        TestUtils.assertThrows(errorMessage, () -> bc.insertBlock(genesis2), IllegalStateException.class);
    }

    @Test
    public void getCurrentHead() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        assertEquals(genesis, bc.getCurrentHead());

        Block prev = genesis;

        for (int i = 0; i < 5; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        Block newHead = prev;
        assertEquals(newHead, bc.getCurrentHead());

        prev = genesis;

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(newHead, bc.getCurrentHead());

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(prev, bc.getCurrentHead());
        assertNotEquals(bc.getCurrentHead(), newHead);
    }

    @Test
    public void getAncestorsStartingAt() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        assertEquals(genesis, bc.getCurrentHead());

        Block prev = genesis;
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(prev);

        for (int i = 0; i < 5; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(0, next);
            prev = next;
        }

        Block newHead = prev;
        assertEquals(blocks, bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix()));

        assertEquals(blocks.subList(0,2),
                     bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix(),2));

        assertEquals(blocks.subList(1,3),
                     bc.getAncestorsStartingAt(blocks.get(1).getShaTwoFiftySix(),2));

        prev = genesis;
        blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(0, next);
            prev = next;
        }

        assertEquals(blocks, bc.getAncestorsStartingAt(prev.getShaTwoFiftySix()));

        assertEquals(errorMessage, new ArrayList<>(), bc.getAncestorsStartingAt(null));

        assertEquals(errorMessage, new ArrayList<>(), bc.getAncestorsStartingAt(null, -1));

        assertEquals(errorMessage, new ArrayList<>(),
                     bc.getAncestorsStartingAt(prev.getShaTwoFiftySix(), -1));

        assertEquals(errorMessage, new ArrayList<>(), bc.getAncestorsStartingAt(null, 10));

    }

    @Test
    public void containsBlockWithHash() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (Block b: blocks) {
            Assert.assertTrue(bc.containsBlockWithHash(b.getShaTwoFiftySix()));
        }
    }

    @Test
    public void containsBlock() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        Assert.assertTrue(bc.containsBlock(genesis));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (Block b: blocks) {
            Assert.assertTrue(bc.containsBlock(b));
        }
    }

    @Test
    public void importMainChain() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = Crypto.signatureKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }
        bc.storeBlockChain();
        BlockChain newbc = new BlockChain();
        newbc.importBlockChain(new File("blockchain" + bc.getCurrentHead().getShaTwoFiftySix()));
        for (Block b : blocks) {
            Assert.assertTrue(newbc.containsBlock(b));
        }

        newbc.destroyBlockchain(new File("blockchain" + bc.getCurrentHead().getShaTwoFiftySix()));
    }
}


