package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import testutils.RandomizedTest;
import transaction.Transaction;
import utils.Config;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(JUnitQuickcheck.class)
public class BlockProperties extends RandomizedTest {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }


    @BeforeClass
    public static void setHashGoal() {
        Config.setHashGoal(1);
    }

    @Property(trials = 5)
    public void findValidNonceFindsValidNonce(ECDSAKeyPair reward, Transaction txA, Transaction txB) throws Exception {
        Block b = Block.empty(randomShaTwoFiftySix());
        b.addReward(reward.publicKey);
        b.addTransaction(txA);
        b.addTransaction(txB);

        b.findValidNonce(new AtomicBoolean(false));
        Assert.assertTrue(b.checkHash());
    }
}
