package block;

import org.junit.Test;
import testutils.RandomizedTest;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by eperdew on 3/5/17.
 */
public class BlockChainTest extends RandomizedTest {

    @Test
    public void getBlockWithHash() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = Crypto.signatureKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        assertEquals(bc.getBlockWithHash(genesis.getShaTwoFiftySix()), Optional.of(genesis));

        Block prev = genesis;

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            assertEquals(bc.getBlockWithHash(next.getShaTwoFiftySix()), Optional.of(next));
            prev = next;
        }
    }

    @Test
    public void insertBlock() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(Crypto.signatureKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        ShaTwoFiftySix randomHash = randomShaTwoFiftySix();
        Block b = randomBlock(randomHash);

        assertEquals(bc.insertBlock(b), false);
        assertEquals(bc.getBlockWithHash(b.getShaTwoFiftySix()), Optional.empty());
    }

    @Test
    public void getCurrentHead() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(Crypto.signatureKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        assertEquals(bc.getCurrentHead(), genesis);

        Block prev = genesis;

        for (int i = 0; i < 5; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        Block newHead = prev;
        assertEquals(bc.getCurrentHead(), newHead);

        prev = genesis;

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(bc.getCurrentHead(), newHead);

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            prev = next;
        }

        assertEquals(bc.getCurrentHead(), prev);
        assertNotEquals(bc.getCurrentHead(), newHead);
    }

    @Test
    public void getAncestorsStartingAt() throws Exception {
        Block genesis = Block.genesis();
        genesis.addReward(Crypto.signatureKeyPair().getPublic());
        BlockChain bc = new BlockChain(genesis);

        assertEquals(bc.getCurrentHead(), genesis);

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
        assertEquals(bc.getAncestorsStartingAt(newHead.getShaTwoFiftySix()), blocks);

        prev = genesis;
        blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 3; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(0, next);
            prev = next;
        }

        assertEquals(bc.getAncestorsStartingAt(prev.getShaTwoFiftySix()), blocks);
    }

    @Test
    public void containsBlockWithHash() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = Crypto.signatureKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        assertEquals(bc.containsBlockWithHash(genesis.getShaTwoFiftySix()), true);

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(genesis);
            prev = next;
        }

        for (Block b: blocks) {
            assertEquals(bc.containsBlockWithHash(b.getShaTwoFiftySix()), true);
        }
    }

    @Test
    public void containsBlock() throws Exception {
        Block genesis = Block.genesis();
        PublicKey PK = Crypto.signatureKeyPair().getPublic();
        genesis.addReward(PK);
        BlockChain bc = new BlockChain(genesis);
        assertEquals(bc.containsBlock(genesis), true);

        Block prev = genesis;
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesis);

        for (int i = 0; i < 100; ++i) {
            Block next = randomBlock(prev.getShaTwoFiftySix());
            bc.insertBlock(next);
            blocks.add(genesis);
            prev = next;
        }

        for (Block b: blocks) {
            assertEquals(bc.containsBlock(b), true);
        }
    }
}