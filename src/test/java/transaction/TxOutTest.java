package transaction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.Crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class TxOutTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws GeneralSecurityException, IOException {
        KeyPair pair = Crypto.signatureKeyPair();
        TxOut output = new TxOut(100, pair.getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        output.serialize(new DataOutputStream(outputStream));
        TxOut deserialized = TxOut.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(output.ownerPubKey, deserialized.ownerPubKey);
        Assert.assertEquals(output.value, deserialized.value);
    }

    @Test
    public void testEquals() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        long value = random.nextInt(Integer.MAX_VALUE);
        TxOut output1 = new TxOut(value, pair.getPublic());
        TxOut output2 = new TxOut(value, pair.getPublic());

        KeyPair otherPair = Crypto.signatureKeyPair();
        TxOut anotherOutput = new TxOut(random.nextInt(Integer.MAX_VALUE), otherPair.getPublic());
        TxOut output3 = new TxOut(value, otherPair.getPublic());

        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output1);
        TestUtils.assertEqualsWithHashCode(errorMessage, output1, output2);
        Assert.assertNotEquals(errorMessage, output1, anotherOutput);
        Assert.assertNotEquals(errorMessage, output1, null);
        Assert.assertNotEquals(errorMessage, output1, output3);
    }
}
