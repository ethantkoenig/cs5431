package transaction;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class TxInTest extends RandomizedTest {

    @Test
    public void testSerialize() throws Exception {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        TxIn input = new TxIn(hash, 4);

        byte[] serialized = ByteUtil.asByteArray(input::serialize);
        TxIn deserialized = TxIn.DESERIALIZER.deserialize(new DataInputStream(
                new ByteArrayInputStream(serialized)
        ));

        Assert.assertEquals(errorMessage, input.previousTxn, deserialized.previousTxn);
        Assert.assertEquals(errorMessage, input.txIdx, deserialized.txIdx);
    }

    @Test
    public void testEquals() throws Exception {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        TxIn input1 = new TxIn(hash, 4);
        TxIn input2 = new TxIn(hash, 4);
        TxIn input3 = new TxIn(hash, 5);
        TxIn anotherInput = new TxIn(randomShaTwoFiftySix(), random.nextInt(10));

        TestUtils.assertEqualsWithHashCode(errorMessage, input1, input1);
        TestUtils.assertEqualsWithHashCode(errorMessage, input1, input2);
        Assert.assertNotEquals(errorMessage, input1, anotherInput);
        Assert.assertNotEquals(errorMessage, input1, null);
        Assert.assertNotEquals(errorMessage, input1, input3);
    }
}
