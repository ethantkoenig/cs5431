package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PublicKey;

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
    public void testDeserializePublicKey() throws Exception {
        PublicKey publicKey = Crypto.signatureKeyPair().getPublic();

        InputStream serializedKey = new ByteArrayInputStream(publicKey.getEncoded());
        PublicKey deserializedKey = Crypto.deserializePublicKey(serializedKey);
        Assert.assertEquals(publicKey, deserializedKey);
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

        // test that deserialized public keys can correctly verify
        InputStream serializedKey = new ByteArrayInputStream(pair.getPublic().getEncoded());
        PublicKey deserializedKey = Crypto.deserializePublicKey(serializedKey);
        Assert.assertTrue(errorMessage,
                Crypto.verify(content, signature, deserializedKey));
    }

    @Test
    public void testSha256() throws Exception {
        final int len = random.nextInt(1024);
        byte[] content = new byte[len];
        byte[] hash = Crypto.sha256(content);
        Assert.assertEquals(errorMessage, 32, hash.length);
    }
}
