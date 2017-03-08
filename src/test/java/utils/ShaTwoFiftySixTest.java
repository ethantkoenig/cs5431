package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ShaTwoFiftySixTest extends RandomizedTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Crypto.init();
    }

    @Test
    public void testDeserialize() throws GeneralSecurityException {
        byte[] hash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        Assert.assertTrue(errorMessage, Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testCreate() throws GeneralSecurityException {
        byte[] hash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.create(hash);
        Assert.assertArrayEquals(errorMessage, hash, sha256.copyOfHash());
    }

    @Test
    public void testHashOf() throws GeneralSecurityException {
        byte[] content = randomBytes(random.nextInt(1024));
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.hashOf(content);
        Assert.assertTrue(errorMessage,
                Arrays.equals(Crypto.sha256(content), sha256.copyOfHash()));
    }

    @Test
    public void testCopyOfHash() {
        byte[] hash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        byte[] copy = sha256.copyOfHash();
        Assert.assertTrue(errorMessage, Arrays.equals(hash, copy));
        copy[0]++; // should not affect sha256 object
        Assert.assertTrue(errorMessage, Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testWriteTo() throws Exception {
        byte[] hash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);
        ShaTwoFiftySix sha256 = deserializeFrom(hash);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sha256.writeTo(outputStream);
        Assert.assertTrue(errorMessage, Arrays.equals(outputStream.toByteArray(), hash));
    }

    @Test
    public void testEquals() throws GeneralSecurityException {
        byte[] hash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);

        ShaTwoFiftySix hash1 = deserializeFrom(hash);
        ShaTwoFiftySix hash2 = deserializeFrom(hash);

        TestUtils.assertEqualsWithHashCode(errorMessage, hash1, hash1);
        TestUtils.assertEqualsWithHashCode(errorMessage, hash1, hash2);
        Assert.assertNotEquals(errorMessage, hash1, null);

        byte[] anotherHash = randomBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES);
        if (Arrays.equals(hash, anotherHash)) { // sanity-check in case there is a collision
            anotherHash[0]++;
        }
        Assert.assertNotEquals(errorMessage,
                deserializeFrom(hash),
                deserializeFrom(anotherHash));
    }

    @Test
    public void testCheckHashZeros() {
        byte[] hash = new byte[ShaTwoFiftySix.HASH_SIZE_IN_BYTES];
        hash[2] = 0x01;
        ShaTwoFiftySix shaTwoFiftySix = ShaTwoFiftySix.create(hash);
        Assert.assertTrue(errorMessage, shaTwoFiftySix.checkHashZeros(1));
        Assert.assertTrue(errorMessage, shaTwoFiftySix.checkHashZeros(2));
        Assert.assertFalse(errorMessage, shaTwoFiftySix.checkHashZeros(3));
    }

    private ShaTwoFiftySix deserializeFrom(byte[] hash) {
        return ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
    }
}
