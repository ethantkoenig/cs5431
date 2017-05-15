package crypto;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import testutils.TestUtils;
import utils.ByteUtil;

@RunWith(JUnitQuickcheck.class)
public class ECDSASignatureProperties {

    @Property(trials = 3)
    public void serializeDeserialize(ECDSASignature signature) throws Exception {
        byte[] serialized = ByteUtil.asByteArray(signature::serialize);
        ECDSASignature deserialized = ECDSASignature.DESERIALIZER.deserialize(serialized);
        TestUtils.assertEqualsWithHashCode(signature, deserialized);
    }
}
