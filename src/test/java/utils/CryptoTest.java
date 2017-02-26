package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;

import java.security.KeyPair;

public class CryptoTest extends RandomizedTest {

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
        final int len = random.nextInt(1024);
        byte[] content = new byte[len];
        random.nextBytes(content);

        KeyPair pair = Crypto.signatureKeyPair();
        byte[] signature = Crypto.sign(content, pair.getPrivate());
        Assert.assertTrue(errorMessage,
                Crypto.verify(content, signature, pair.getPublic()));
    }

    @Test
    public void testSha256() throws Exception {
        final int len = random.nextInt(1024);
        byte[] content = new byte[len];
        byte[] hash = Crypto.sha256(content);
        Assert.assertEquals(errorMessage, 32, hash.length);
    }
}
