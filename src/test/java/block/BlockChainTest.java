package block;

import org.junit.Test;
import testutils.RandomizedTest;
import utils.Crypto;

import java.security.PublicKey;
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



    }

    @Test
    public void getCurrentHead() throws Exception {

    }

    @Test
    public void getAncestorsStartingAt() throws Exception {

    }

    @Test
    public void containsBlockWithHash() throws Exception {

    }

    @Test
    public void containsBlock() throws Exception {

    }
}