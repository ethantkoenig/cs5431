package cli;

import crypto.Crypto;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import crypto.ECDSASignature;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.File;

public class GenerateKeyTest extends RandomizedTest {

    @Test
    public void testGenerateKey() throws Exception {
        File privateFile = File.createTempFile("private", ".tmp");
        File publicFile = File.createTempFile("public", ".tmp");
        GenerateKey generateKey = new GenerateKey(crypto);
        generateKey.generateKey(privateFile.getAbsolutePath(), publicFile.getAbsolutePath());

        ECDSAPrivateKey privateKey = Crypto.loadPrivateKey(privateFile.getAbsolutePath());
        ECDSAPublicKey publicKey = Crypto.loadPublicKey(publicFile.getAbsolutePath());

        byte[] content = randomBytes(random.nextInt(1024));
        ECDSASignature signature = Crypto.sign(content, privateKey);
        Assert.assertTrue(errorMessage, Crypto.verify(content, signature, publicKey));
    }
}
