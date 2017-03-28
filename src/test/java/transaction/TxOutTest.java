package transaction;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.Crypto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class TxOutTest extends RandomizedTest {

    @Test
    public void testSerialize() throws Exception {
        KeyPair pair = randomKeyPair();
        TxOut output = new TxOut(100, pair.getPublic());

        byte[] serialized = ByteUtil.asByteArray(output::serialize);
        TxOut deserialized = TxOut.DESERIALIZER.deserialize(new DataInputStream(
                new ByteArrayInputStream(serialized)
        ));

        Assert.assertEquals(output.ownerPubKey, deserialized.ownerPubKey);
        Assert.assertEquals(output.value, deserialized.value);
    }

    @Test
    public void testEquals() throws Exception {
        KeyPair pair = randomKeyPair();
        long value = random.nextInt(Integer.MAX_VALUE);
        TxOut output1 = new TxOut(value, pair.getPublic());
        TxOut output2 = new TxOut(value, pair.getPublic());

        KeyPair otherPair = randomKeyPair();
        TxOut anotherOutput = new TxOut(random.nextInt(Integer.MAX_VALUE), otherPair.getPublic());
        TxOut output3 = new TxOut(value, otherPair.getPublic());

        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output1);
        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output2);
        Assert.assertNotEquals(errorMessage, output1, anotherOutput);
        Assert.assertNotEquals(errorMessage, output1, null);
        Assert.assertNotEquals(errorMessage, output1, output3);
    }
}
