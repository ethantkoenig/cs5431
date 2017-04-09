package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import testutils.RandomizedTest;
import transaction.Transaction;
import utils.Config;
import utils.Crypto;
import utils.ECDSAKeyPair;

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

    @Property
    public void findValidNonceFindsValidNonce(ECDSAKeyPair reward, Transaction txA, Transaction txB) throws Exception {
        Block b = Block.empty(randomShaTwoFiftySix());
        b.addReward(reward.publicKey);
        b.addTransaction(txA);
        b.addTransaction(txB);

        b.findValidNonce(new AtomicBoolean(false));
        Assert.assertTrue(b.checkHash());
    }
}
