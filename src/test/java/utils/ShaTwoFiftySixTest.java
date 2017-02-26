package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ShaTwoFiftySixTest extends RandomizedTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Crypto.init();
    }

    @Test
    public void testDeserialize() throws GeneralSecurityException {
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
        Assert.assertTrue(errorMessage, Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testHashOf() throws GeneralSecurityException {
        byte[] content = new byte[random.nextInt(1024)];
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.hashOf(content);
        Assert.assertTrue(errorMessage,
                Arrays.equals(Crypto.sha256(content), sha256.copyOfHash()));
    }

    @Test
    public void testCopyOfHash() {
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
        byte[] copy1 = sha256.copyOfHash();
        copy1[0]++; // should not affect sha256 object
        Assert.assertTrue(errorMessage,
                Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testEquals() throws GeneralSecurityException {
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(hash);

        Assert.assertEquals(errorMessage,
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)));

        byte[] anotherHash = new byte[32];
        random.nextBytes(anotherHash);
        if (Arrays.equals(hash, anotherHash)) { // sanity-check in case there is a collision
            anotherHash[0]++;
        }
        Assert.assertNotEquals(errorMessage,
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(anotherHash)));
    }

    @Test
    public void testHashCode() throws GeneralSecurityException {
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(hash);
        Assert.assertEquals(errorMessage,
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)).hashCode(),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)).hashCode());
    }
}
