package block;

import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;
import utils.ShaTwoFiftySix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class BlockChainTest extends RandomizedTest {

    @Test
    public void testVerify() throws Exception {
        // Test getUnspentTransactionsAt(...) and verifyNonGenesisBlock(...)
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        assertEquals(UnspentTransactions.empty(),
                bc.getUnspentTransactionsAt(randomBlock(randomShaTwoFiftySix())));

        ECDSAKeyPair senderPair = crypto.signatureKeyPair();
        ECDSAKeyPair recipientPair = crypto.signatureKeyPair();

        Block genesis = Block.genesis(senderPair.publicKey);
        genesis.findValidNonce();
        Assert.assertTrue(bc.insertBlock(genesis));

        Assert.assertTrue(errorMessage, genesis.verifyGenesis(senderPair.publicKey));
        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        List<Transaction> transactions = new ArrayList<>();
        ShaTwoFiftySix prevTxOut = genesis.getShaTwoFiftySix();
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            Transaction tx = new Transaction.Builder()
                    .addInput(new TxIn(prevTxOut, 0), senderPair.privateKey)
                    .addOutput(new TxOut(Block.REWARD_AMOUNT - (i + 1), senderPair.publicKey))
                    .addOutput(new TxOut(1, recipientPair.publicKey))
                    .build();
            transactions.add(tx);
            unspentTxs.put(tx.getShaTwoFiftySix(), 1, tx.getOutput(1));
            prevTxOut = tx.getShaTwoFiftySix();
        }

        Block next = Block.block(genesis.getShaTwoFiftySix(), transactions, senderPair.publicKey);
        next.findValidNonce();

        unspentTxs.put(prevTxOut, 0,
                next.transactions[Block.NUM_TRANSACTIONS_PER_BLOCK - 1].getOutput(0));
        unspentTxs.put(next.getShaTwoFiftySix(), 0, next.reward);

        Assert.assertTrue(errorMessage, bc.verifyNonGenesisBlock(next).isPresent());
        bc.insertBlock(next);
        Assert.assertTrue(errorMessage, bc.verifyNonGenesisBlock(next).isPresent());
        assertEquals(errorMessage, unspentTxs, bc.getUnspentTransactionsAt(next));

        Assert.assertFalse(errorMessage, bc.verifyNonGenesisBlock(randomBlock(randomShaTwoFiftySix())).isPresent());


        Block fauxGenesis = new Block(ShaTwoFiftySix.zero(), new Transaction[0],
                new TxOut(Block.REWARD_AMOUNT + 1, crypto.signatureKeyPair().publicKey));
        Assert.assertFalse(errorMessage, bc.verifyNonGenesisBlock(fauxGenesis).isPresent());
    }

    @Test
    public void getBlockWithHash() throws Exception {
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
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
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

        ShaTwoFiftySix randomHash = randomShaTwoFiftySix();
        Block b = randomBlock(randomHash);

        Assert.assertFalse(errorMessage, bc.insertBlock(b));
        assertEquals(errorMessage, Optional.empty(), bc.getBlockWithHash(b.getShaTwoFiftySix()));
    }

    @Test
    public void insertBlockEmpty() throws Exception {
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        assertFalse(errorMessage, bc.insertBlock(randomBlock(randomShaTwoFiftySix())));

        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        assertTrue(errorMessage, bc.insertBlock(genesis));
        assertTrue(errorMessage, bc.containsBlock(genesis));
        assertEquals(errorMessage, genesis, bc.getCurrentHead());
    }

    @Test
    public void insertDuplicateGenesis() throws Exception {
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"));
        ECDSAPublicKey key1 = crypto.signatureKeyPair().publicKey;
        ECDSAPublicKey key2 = crypto.signatureKeyPair().publicKey;

        Block genesis1 = Block.genesis(key1);
        Block genesis2 = Block.genesis(key2);

        assertTrue(bc.insertBlock(genesis1));
        assertFalse(bc.insertBlock(genesis2));
    }

    @Test
    public void getCurrentHead() throws Exception {
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

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
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);

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

        assertEquals(blocks.subList(0, 2),
                bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix(), 2));

        assertEquals(blocks.subList(1, 3),
                bc.getAncestorsStartingAt(blocks.get(1).getShaTwoFiftySix(), 2));

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
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (Block b : blocks) {
            Assert.assertTrue(bc.containsBlockWithHash(b.getShaTwoFiftySix()));
        }
    }

    @Test
    public void containsBlock() throws Exception {
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        BlockChain bc = new BlockChain(Files.createTempDirectory("test"), genesis);
        Assert.assertTrue(bc.containsBlock(genesis));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        for (Block b : blocks) {
            Assert.assertTrue(bc.containsBlock(b));
        }
    }

    @Test
    public void importMainChain() throws Exception {
        Config.setHashGoal(1);
        Block genesis = Block.genesis(crypto.signatureKeyPair().publicKey);
        genesis.findValidNonce();
        Path blockChainPath = Files.createTempDirectory("test");
        BlockChain bc = new BlockChain(blockChainPath, genesis);
        Assert.assertTrue(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()));

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 10; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            next.findValidNonce();
            bc.insertBlock(next);
            blocks.add(next);
            prev = next;
        }

        BlockChain newBlockChain = new BlockChain(blockChainPath);
        for (Block b : blocks) {
            Assert.assertTrue(newBlockChain.containsBlock(b));
        }
    }
}


