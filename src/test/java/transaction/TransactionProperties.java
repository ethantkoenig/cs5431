package transaction;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import utils.ByteUtil;

import static testutils.TestUtils.assertEqualsWithHashCode;

@RunWith(JUnitQuickcheck.class)
public class TransactionProperties {
    @Property(trials = 5)
    public void deserializeSerializeInverse(Transaction tx) throws Exception {
        byte[] ser = ByteUtil.asByteArray(tx::serialize);
        Transaction deser = Transaction.DESERIALIZER.deserialize(ser);

        assertEqualsWithHashCode(
                "Transaction deserialized to something different after serialization",
                tx, deser);
    }
}
