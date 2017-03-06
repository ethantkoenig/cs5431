package transaction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class RTxInTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws GeneralSecurityException, IOException {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        RTxIn input = new RTxIn(hash, 4);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        input.serialize(new DataOutputStream(outputStream));
        RTxIn deserialized = RTxIn.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(errorMessage, input.previousTxn, deserialized.previousTxn);
        Assert.assertEquals(errorMessage, input.txIdx, deserialized.txIdx);
    }

    @Test
    public void testEquals() throws Exception {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        RTxIn input1 = new RTxIn(hash, 4);
        RTxIn input2 = new RTxIn(hash, 4);
        RTxIn anotherInput = new RTxIn(randomShaTwoFiftySix(), random.nextInt(10));

        Assert.assertEquals(errorMessage, input1, input1);
        Assert.assertEquals(errorMessage, input1, input2);
        Assert.assertNotEquals(errorMessage, input1, anotherInput);
        Assert.assertNotEquals(errorMessage, input1, null);
    }

    @Test
    public void testHashCode() throws Exception {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        int index = random.nextInt(16);
        RTxIn input1 = new RTxIn(hash, index);
        RTxIn input2 = new RTxIn(hash, index);
        Assert.assertEquals(errorMessage, input1.hashCode(), input2.hashCode());
    }
}
