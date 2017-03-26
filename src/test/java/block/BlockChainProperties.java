package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import utils.Config;
import utils.Crypto;

@RunWith(JUnitQuickcheck.class)
public class BlockChainProperties {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Before
    public void setConfig() {
        Config.HASH_GOAL.set(0);

    }

    @Property(trials = 10)
    public void deserializeSerializeInverse(BlockChain blockchain) throws Exception {
        // TODO
    }
}
