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
}
