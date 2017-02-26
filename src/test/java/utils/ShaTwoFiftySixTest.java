package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.SeededRandom;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;

public class ShaTwoFiftySixTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Crypto.init();
    }

    @Test
    public void testDeserialize() throws GeneralSecurityException {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        byte[] hash = new byte[32];
        rand.nextBytes(hash);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
        Assert.assertTrue(seededRand.errorMessage(), Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testHashOf() throws GeneralSecurityException {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        byte[] content = new byte[rand.nextInt(1024)];
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.hashOf(content);
        Assert.assertTrue(seededRand.errorMessage(),
                Arrays.equals(Crypto.sha256(content), sha256.copyOfHash()));
    }

    @Test
    public void testCopyOfHash() {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        byte[] hash = new byte[32];
        rand.nextBytes(hash);
        ShaTwoFiftySix sha256 = ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash));
        byte[] copy1 = sha256.copyOfHash();
        copy1[0]++; // should not affect sha256 object
        Assert.assertTrue(seededRand.errorMessage(),
                Arrays.equals(hash, sha256.copyOfHash()));
    }

    @Test
    public void testEquals() throws GeneralSecurityException {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        byte[] hash = new byte[32];
        rand.nextBytes(hash);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(hash);

        Assert.assertEquals(seededRand.errorMessage(),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)));

        byte[] anotherHash = new byte[32];
        rand.nextBytes(anotherHash);
        if (Arrays.equals(hash, anotherHash)) { // sanity-check in case there is a collision
            anotherHash[0]++;
        }
        Assert.assertNotEquals(seededRand.errorMessage(),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(anotherHash)));
    }

    @Test
    public void testHashCode() throws GeneralSecurityException {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        byte[] hash = new byte[32];
        rand.nextBytes(hash);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(hash);
        Assert.assertEquals(seededRand.errorMessage(),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)).hashCode(),
                ShaTwoFiftySix.deserialize(ByteBuffer.wrap(hash)).hashCode());
    }

}
