package transaction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class TxInTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws GeneralSecurityException, IOException {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        TxIn input = new TxIn(hash, 4);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        input.serialize(new DataOutputStream(outputStream));
        TxIn deserialized = TxIn.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

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
