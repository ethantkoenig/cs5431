package transaction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.Crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class RTxOutTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws GeneralSecurityException, IOException {
        KeyPair pair = Crypto.signatureKeyPair();
        RTxOut output = new RTxOut(100, pair.getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        output.serialize(new DataOutputStream(outputStream));
        RTxOut deserialized = RTxOut.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(output.ownerPubKey, deserialized.ownerPubKey);
        Assert.assertEquals(output.value, deserialized.value);
    }

    @Test
    public void testEquals() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        long value = random.nextInt(Integer.MAX_VALUE);
        RTxOut output1 = new RTxOut(value, pair.getPublic());
        RTxOut output2 = new RTxOut(value, pair.getPublic());

        KeyPair otherPair = Crypto.signatureKeyPair();
        RTxOut anotherOutput = new RTxOut(random.nextInt(Integer.MAX_VALUE), otherPair.getPublic());

        Assert.assertEquals(errorMessage, output1, output2);
        Assert.assertNotEquals(errorMessage, output1, anotherOutput);
        Assert.assertNotEquals(errorMessage, output1, null);
    }

    @Test
    public void testHashCode() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        long value = random.nextInt(Integer.MAX_VALUE);
        RTxOut output1 = new RTxOut(value, pair.getPublic());
        RTxOut output2 = new RTxOut(value, pair.getPublic());
        Assert.assertEquals(errorMessage, output1.hashCode(), output2.hashCode());
    }
}
