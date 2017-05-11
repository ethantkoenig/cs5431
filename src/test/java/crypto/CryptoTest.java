package crypto;

import org.junit.Assert;
import org.junit.Test;
import testutils.Fixtures;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;
import utils.Config;

import java.util.Base64;


public class CryptoTest extends RandomizedTest {
    @Test
    public void testHashAndSalt() throws Exception {
        Config.setPbkdf2Cost(5);

        String pass = "password";
        byte[] salt = crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(pass, salt);

        Assert.assertArrayEquals(hash, Crypto.pbkdf2(pass, salt));
    }

    @Test
    public void testSignatureKeyPair() throws Exception {
        Assert.assertNotNull(crypto.signatureKeyPair());
    }

    @Test
    public void testDeserializePublicKey() throws Exception {
        ECDSAPublicKey publicKey = crypto.signatureKeyPair().publicKey;

        ECDSAPublicKey deserializedKey = ECDSAPublicKey.DESERIALIZER.deserialize(
                ByteUtil.asByteArray(publicKey::serialize)
        );
        TestUtils.assertEqualsWithHashCode(errorMessage, publicKey, deserializedKey);
    }

    @Test
    public void testSign() throws Exception {
        byte[] content = randomBytes(random.nextInt(1024));

        ECDSAKeyPair pair = crypto.signatureKeyPair();
        ECDSASignature signature = crypto.sign(content, pair.privateKey);
        Assert.assertTrue(errorMessage,
                Crypto.verify(content, signature, pair.publicKey));

        byte[] otherContent = randomBytes(random.nextInt(1024));
        Assert.assertFalse(errorMessage,
                Crypto.verify(otherContent, signature, pair.publicKey));

        // test that deserialized public keys can correctly verify
        ECDSAPublicKey deserializedKey = ECDSAPublicKey.DESERIALIZER.deserialize(
                ByteUtil.asByteArray(pair.publicKey::serialize)
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

    @Test
    public void testTemp() throws Exception {
        byte[] salt = crypto.generateSalt();
        System.out.println(Base64.getMimeEncoder().encodeToString(salt));
        byte[] hashedPassword = Crypto.hashAndSalt(Fixtures.USER_PASSWORD, salt);
        System.out.println(Base64.getMimeEncoder().encodeToString(hashedPassword));
    }
}
