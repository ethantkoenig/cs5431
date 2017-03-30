package transaction;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import utils.ByteUtil;
import utils.Crypto;

import static testutils.TestUtils.assertEqualsWithHashCode;

@RunWith(JUnitQuickcheck.class)
public class TransactionProperties {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Property
    public void deserializeSerializeInverse(Transaction tx) throws Exception {
        byte[] ser = ByteUtil.asByteArray(tx::serialize);
        Transaction deser = Transaction.DESERIALIZER.deserialize(ser);

        assertEqualsWithHashCode(
                "Transaction deserialized to something different after serialization",
                tx, deser);
    }
}
