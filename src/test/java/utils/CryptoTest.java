package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.SeededRandom;

import java.security.KeyPair;
import java.util.Random;

public class CryptoTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Crypto.init();
    }

    @Test
    public void testSignatureKeyPair() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        Assert.assertNotNull(pair);
    }

    @Test
    public void testSign() throws Exception {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        final int len = rand.nextInt(1024);
        byte[] content = new byte[len];
        rand.nextBytes(content);

        KeyPair pair = Crypto.signatureKeyPair();
        byte[] signature = Crypto.sign(content, pair.getPrivate());
        Assert.assertTrue(seededRand.errorMessage(),
                Crypto.verify(content, signature, pair.getPublic()));
    }

    @Test
    public void testSha256() throws Exception {
        SeededRandom seededRand = SeededRandom.randomSeed();
        Random rand = seededRand.random();

        final int len = rand.nextInt(1024);
        byte[] content = new byte[len];
        ShaTwoFiftySix hash = Crypto.sha256(content);
        Assert.assertEquals(seededRand.errorMessage(), 32, hash.copyOfHash().length);
    }
}
