package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.PublicKey;

public class CryptoTest extends RandomizedTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Crypto.init();
    }

    @Test
    public void testHashAndSalt() throws Exception {
        Config.PBKDF2_COST.set(5);

        String pass = "password";
        byte[] salt = Crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(pass, salt);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(pass.getBytes("UTF-8"));
        outputStream.write(salt);
        Assert.assertArrayEquals(hash, Crypto.pbkdf2(outputStream.toByteArray(), salt));
    }

    @Test
    public void testSignatureKeyPair() throws Exception {
        KeyPair pair = Crypto.signatureKeyPair();
        Assert.assertNotNull(pair);
    }

    @Test
    public void testDeserializePublicKey() throws Exception {
        PublicKey publicKey = Crypto.signatureKeyPair().getPublic();

        PublicKey deserializedKey = Crypto.deserializePublicKey(
                new ByteArrayInputStream(publicKey.getEncoded())
        );
        Assert.assertEquals(publicKey, deserializedKey);
    }

    @Test
    public void testSign() throws Exception {
        byte[] content = randomBytes(random.nextInt(1024));

        KeyPair pair = Crypto.signatureKeyPair();
        byte[] signature = Crypto.sign(content, pair.getPrivate());
        Assert.assertTrue(errorMessage,
                Crypto.verify(content, signature, pair.getPublic()));

        // test that deserialized public keys can correctly verify
        PublicKey deserializedKey = Crypto.deserializePublicKey(
                new ByteArrayInputStream(pair.getPublic().getEncoded())
        );
        Assert.assertTrue(errorMessage,
                Crypto.verify(content, signature, deserializedKey));
    }

    @Test
    public void testSha256() throws Exception {
        byte[] content = randomBytes(random.nextInt(1024));
        byte[] hash = Crypto.sha256(content);
        Assert.assertEquals(errorMessage, 32, hash.length);
    }
}
