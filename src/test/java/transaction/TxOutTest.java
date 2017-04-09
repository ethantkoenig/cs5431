package transaction;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.Crypto;
import utils.ECDSAKeyPair;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class TxOutTest extends RandomizedTest {

    @Test
    public void testSerialize() throws Exception {
        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        TxOut output = new TxOut(100, pair.publicKey);

        byte[] serialized = ByteUtil.asByteArray(output::serialize);
        TxOut deserialized = TxOut.DESERIALIZER.deserialize(new DataInputStream(
                new ByteArrayInputStream(serialized)
        ));

        Assert.assertEquals(output.ownerPubKey, deserialized.ownerPubKey);
        Assert.assertEquals(output.value, deserialized.value);
    }

    @Test
    public void testEquals() throws Exception {
        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        long value = random.nextInt(Integer.MAX_VALUE);
        TxOut output1 = new TxOut(value, pair.publicKey);
        TxOut output2 = new TxOut(value, pair.publicKey);

        ECDSAKeyPair otherPair = Crypto.signatureKeyPair();
        TxOut anotherOutput = new TxOut(random.nextInt(Integer.MAX_VALUE), otherPair.publicKey);
        TxOut output3 = new TxOut(value, otherPair.publicKey);

        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output1);
        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output2);
        Assert.assertNotEquals(errorMessage, output1, anotherOutput);
        Assert.assertNotEquals(errorMessage, output1, null);
        Assert.assertNotEquals(errorMessage, output1, output3);
    }
}
