package crypto;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import testutils.TestUtils;
import utils.ByteUtil;

@RunWith(JUnitQuickcheck.class)
public class ECDSAPublicKeyProperties {

    @Property(trials = 3)
    public void serializeDeserialize(ECDSAKeyPair pair) throws Exception {
        ECDSAPublicKey key = pair.publicKey;
        byte[] serialized = ByteUtil.asByteArray(key::serialize);
        ECDSAPublicKey deserialized = ECDSAPublicKey.DESERIALIZER.deserialize(serialized);
        TestUtils.assertEqualsWithHashCode(key, deserialized);
    }
}
