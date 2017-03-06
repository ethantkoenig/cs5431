package cli;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.Crypto;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

public class GenerateKeyTest extends RandomizedTest {

    @Test
    public void testGenerateKey() throws Exception {
        File privateFile = File.createTempFile("private", ".tmp");
        File publicFile = File.createTempFile("public", ".tmp");
        GenerateKey.generateKey(privateFile.getAbsolutePath(), publicFile.getAbsolutePath());

        PrivateKey privateKey = Crypto.loadPrivateKey(privateFile.getAbsolutePath());
        PublicKey publicKey = Crypto.loadPublicKey(publicFile.getAbsolutePath());

        byte[] content = randomBytes(random.nextInt(1024));
        byte[] signature = Crypto.sign(content, privateKey);
        Assert.assertTrue(errorMessage, Crypto.verify(content, signature, publicKey));
    }
}
