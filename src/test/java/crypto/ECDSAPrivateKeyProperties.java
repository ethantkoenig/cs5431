package crypto;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import testutils.TestUtils;
import utils.ByteUtil;

@RunWith(JUnitQuickcheck.class)
public class ECDSAPrivateKeyProperties {

    @Property(trials = 3)
    public void serializeDeserialize(ECDSAKeyPair pair) throws Exception {
        ECDSAPrivateKey key = pair.privateKey;
        byte[] serialized = ByteUtil.asByteArray(key::serialize);
        ECDSAPrivateKey deserialized = ECDSAPrivateKey.DESERIALIZER.deserialize(serialized);
        TestUtils.assertEqualsWithHashCode(key, deserialized);

    }
}
