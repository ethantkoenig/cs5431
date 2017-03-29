package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import testutils.RandomizedTest;
import transaction.Transaction;
import utils.Config;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;
import java.util.List;

@RunWith(JUnitQuickcheck.class)
public class BlockProperties extends RandomizedTest {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }


    @BeforeClass
    public static void setHashGoal() {
        Config.HASH_GOAL.set(2);
    }

    @Property
    public void findValidNonceFindsValidNonce(KeyPair reward, Transaction txA, Transaction txB) throws Exception {
        Block b = Block.empty(randomShaTwoFiftySix());
        b.addReward(reward.getPublic());
        b.addTransaction(txA);
        b.addTransaction(txB);

        b.findValidNonce();
        Assert.assertTrue(b.checkHash());
    }
}
