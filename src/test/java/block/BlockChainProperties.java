package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import utils.Crypto;

@RunWith(JUnitQuickcheck.class)
public class BlockChainProperties {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Property(trials = 1)
    public void deserializeSerializeInverse(BlockChain blockchain) throws Exception {
        // TODO
    }
}
