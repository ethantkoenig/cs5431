package utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;

public class CryptoTest extends RandomizedTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Crypto.init();
    }

    @Test
    public void testHashAndSalt() throws Exception {
        Config.setPbkdf2Cost(5);

        String pass = "password";
        byte[] salt = Crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(pass, salt);

        Assert.assertArrayEquals(hash, Crypto.pbkdf2(pass, salt));
    }

    @Test
    public void testSignatureKeyPair() throws Exception {
        Assert.assertNotNull(Crypto.signatureKeyPair());
    }

    @Test
    public void testDeserializePublicKey() throws Exception {
        ECDSAPublicKey publicKey = Crypto.signatureKeyPair().publicKey;

        ECDSAPublicKey deserializedKey = ECDSAPublicKey.DESERIALIZER.deserialize(
                ByteUtil.asByteArray(publicKey::serialize)
        );
        TestUtils.assertEqualsWithHashCode(errorMessage, publicKey, deserializedKey);
    }

    @Test
    public void testSign() throws Exception {
        byte[] content = randomBytes(random.nextInt(1024));

        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        ECDSASignature signature = Crypto.sign(content, pair.privateKey);
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
}
