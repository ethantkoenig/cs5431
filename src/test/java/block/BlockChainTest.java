package block;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Created by eperdew on 3/5/17.
 */
public class BlockChainTest extends RandomizedTest {

    @Test
    public void testVerify() throws Exception {
        // Test getUnspentTransactionsAt(...) and verifyBlock(...)
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        assertEquals(UnspentTransactions.empty(),
                bc.getUnspentTransactionsAt(randomBlock(randomShaTwoFiftySix())));

        KeyPair senderPair = randomKeyPair();
        KeyPair recipientPair = randomKeyPair();

        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(senderPair.getPublic());
        Assert.assertTrue(bc.insertBlock(genesis));

        Assert.assertTrue(errorMessage, bc.verifyBlock(genesis).isPresent());
        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        MiningBlock next = MiningBlock.empty(genesis.getShaTwoFiftySix());
        next.addReward(senderPair.getPublic());

        ShaTwoFiftySix prevTxOut = genesis.getShaTwoFiftySix();

        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            Transaction tx = new Transaction.Builder()
                    .addInput(new TxIn(prevTxOut, 0), senderPair.getPrivate())
                    .addOutput(new TxOut(MiningBlock.REWARD_AMOUNT - (i + 1), senderPair.getPublic()))
                    .addOutput(new TxOut(1, recipientPair.getPublic()))
                    .build();
            next.addTransaction(tx);
            unspentTxs.put(tx.getShaTwoFiftySix(), 1, tx.getOutput(1));
            prevTxOut = tx.getShaTwoFiftySix();
        }

        next.findValidNonce(new AtomicBoolean(false));

        unspentTxs.put(prevTxOut, 0,
                next.transactions[MiningBlock.NUM_TRANSACTIONS_PER_BLOCK - 1].getOutput(0));
        unspentTxs.put(next.getShaTwoFiftySix(), 0, next.reward);

        Assert.assertTrue(errorMessage, bc.verifyBlock(next).isPresent());
        bc.insertBlock(next);
        Assert.assertTrue(errorMessage, bc.verifyBlock(next).isPresent());
        assertEquals(errorMessage, unspentTxs, bc.getUnspentTransactionsAt(next));

        Assert.assertFalse(errorMessage, bc.verifyBlock(randomBlock(randomShaTwoFiftySix())).isPresent());

        MiningBlock fauxGenesis = MiningBlock.genesis();
        fauxGenesis.reward = new TxOut(MiningBlock.REWARD_AMOUNT + 1, Crypto.signatureKeyPair().getPublic());
        Assert.assertFalse(errorMessage, bc.verifyBlock(fauxGenesis).isPresent());
    }

    @Test
    public void getBlockWithHash() throws Exception {
        MiningBlock genesis = MiningBlock.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
        assertEquals(Optional.of(genesis), bc.getBlockWithHash(genesis.getShaTwoFiftySix()));

        MiningBlock prev = genesis;

        for (int i = 0; i < 100; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            assertEquals(Optional.of(next), bc.getBlockWithHash(next.getShaTwoFiftySix()));
            prev = next;
        }
    }

    @Test
    public void insertBlock() throws Exception {
        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

        ShaTwoFiftySix randomHash = randomShaTwoFiftySix();
        MiningBlock b = randomBlock(randomHash);

        Assert.assertFalse(errorMessage, bc.insertBlock(b));
        assertEquals(errorMessage, Optional.empty(), bc.getBlockWithHash(b.getShaTwoFiftySix()));
    }

    @Test
    public void insertBlockEmpty() throws Exception {
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        assertFalse(errorMessage, bc.insertBlock(randomBlock(randomShaTwoFiftySix())));

        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        assertTrue(errorMessage, bc.insertBlock(genesis));
        assertTrue(errorMessage, bc.containsBlock(genesis));
        assertEquals(errorMessage, genesis, bc.getCurrentHead());
    }

    @Test
    public void insertDuplicateGenesis() throws Exception {
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        PublicKey key1 = randomKeyPair().getPublic();
        PublicKey key2 = randomKeyPair().getPublic();

        MiningBlock genesis1 = MiningBlock.genesis();
        genesis1.addReward(key1);

        MiningBlock genesis2 = MiningBlock.genesis();
        genesis2.addReward(key2);

        assertTrue(bc.insertBlock(genesis1));
        assertFalse(bc.insertBlock(genesis2));
    }

    @Test
    public void getCurrentHead() throws Exception {
        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

        assertEquals(genesis, bc.getCurrentHead());

        MiningBlock prev = genesis;

        for (int i = 0; i < 5; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        MiningBlock newHead = prev;
        assertEquals(newHead, bc.getCurrentHead());

        prev = genesis;

        for (int i = 0; i < 3; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(newHead, bc.getCurrentHead());

        for (int i = 0; i < 3; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(prev, bc.getCurrentHead());
        assertNotEquals(bc.getCurrentHead(), newHead);
    }

    @Test
    public void getAncestorsStartingAt() throws Exception {
        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

        assertEquals(genesis, bc.getCurrentHead());

        MiningBlock prev = genesis;
        ArrayList<MiningBlock> blocks = new ArrayList<>();
        blocks.add(prev);

        for (int i = 0; i < 5; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(0, next);
            prev = next;
        }

        MiningBlock newHead = prev;
        assertEquals(blocks, bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix()));

        assertEquals(blocks.subList(0, 2),
                bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix(), 2));

        assertEquals(blocks.subList(1, 3),
                bc.getAncestorsStartingAt(blocks.get(1).getShaTwoFiftySix(), 2));

        prev = genesis;
        blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 3; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
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
        MiningBlock genesis = MiningBlock.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        MiningBlock prev = genesis;
        List<MiningBlock> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (MiningBlock b : blocks) {
            Assert.assertTrue(bc.containsBlockWithHash(b.getShaTwoFiftySix()));
        }
    }

    @Test
    public void containsBlock() throws Exception {
        MiningBlock genesis = MiningBlock.genesis();
        PublicKey PK = randomKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
        Assert.assertTrue(bc.containsBlock(genesis));

        MiningBlock prev = genesis;
        List<MiningBlock> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (MiningBlock b : blocks) {
            Assert.assertTrue(bc.containsBlock(b));
        }
    }

    @Test
    public void importMainChain() throws Exception {
        Config.HASH_GOAL.set(1);
        MiningBlock genesis = MiningBlock.genesis();
        genesis.addReward(randomKeyPair().getPublic());
        genesis.findValidNonce(new AtomicBoolean(false));
        Path blockChainPath = Files.createTempDirectory("test");
        BlockChain bc = new BlockChain(blockChainPath, genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        MiningBlock prev = genesis;
        List<MiningBlock> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 10; ++i) {
            MiningBlock next = randomBlock(prev.getShaTwoFiftySix());
            next.findValidNonce(new AtomicBoolean(false));
            bc.insertBlock(next);
            blocks.add(next);
            prev = next;
        }

        BlockChain newBlockChain = new BlockChain(blockChainPath);
        for (MiningBlock b : blocks) {
            Assert.assertTrue(newBlockChain.containsBlock(b));
        }
    }
}


