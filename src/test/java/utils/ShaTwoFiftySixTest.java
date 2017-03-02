package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.ByteArrayOutputStream;
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
        byte[] hash = randomHash();
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        Assert.assertTrue(errorMessage, Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testHashOf() throws GeneralSecurityException {
        byte[] content = new byte[random.nextInt(1024)];
        random.nextBytes(content);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.hashOf(content);
        Assert.assertTrue(errorMessage,
                Arrays.equals(Crypto.sha256(content), sha256.copyOfHash()));
    }

    @Test
    public void testCopyOfHash() {
        byte[] hash = randomHash();
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        byte[] copy = sha256.copyOfHash();
        Assert.assertTrue(errorMessage, Arrays.equals(hash, copy));
        copy[0]++; // should not affect sha256 object
        Assert.assertTrue(errorMessage, Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testWriteTo() throws Exception {
        byte[] hash = randomHash();
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sha256.writeTo(outputStream);
        Assert.assertTrue(errorMessage, Arrays.equals(outputStream.toByteArray(), hash));
    }

    @Test
    public void testEquals() throws GeneralSecurityException {
        byte[] hash = randomHash();

        ShaTwoFiftySix hash1 = deserializeFrom(hash);
        ShaTwoFiftySix hash2 = deserializeFrom(hash);

        Assert.assertEquals(errorMessage, hash1, hash1);
        Assert.assertEquals(errorMessage, hash1, hash2);
        Assert.assertNotEquals(errorMessage, hash1, null);

        byte[] anotherHash = randomHash();
        if (Arrays.equals(hash, anotherHash)) { // sanity-check in case there is a collision
            anotherHash[0]++;
        }
        Assert.assertNotEquals(errorMessage,
                deserializeFrom(hash),
                deserializeFrom(anotherHash));
    }

    @Test
    public void testHashCode() throws GeneralSecurityException {
        byte[] hash = randomHash();
        Assert.assertEquals(errorMessage,
                deserializeFrom(hash).hashCode(),
                deserializeFrom(hash).hashCode());
    }

    private byte[] randomHash() {
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        return hash;
    }

    private ShaTwoFiftySix deserializeFrom(byte[] hash) {
        return ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
    }
}
