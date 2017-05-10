package cli;


import com.google.inject.Inject;
import crypto.Crypto;
import crypto.ECDSAKeyPair;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public class GenerateKey {

    private final Crypto crypto;

    @Inject
    public GenerateKey(Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * Generate a public/private key pair, and write the keys to
     * {@code privateFilename} and {@code publicFilename}
     *
     * @param privateFilename filename/path for private key
     * @param publicFilename  filename/path for public key
     */
    public void generateKey(String privateFilename, String publicFilename)
            throws GeneralSecurityException, IOException {
        ECDSAKeyPair pair = crypto.signatureKeyPair();

        OutputStream privateOutput = new FileOutputStream(privateFilename);
        try {
            pair.privateKey.serialize(new DataOutputStream(privateOutput));
        } finally {
            privateOutput.close();
        }

        OutputStream publicOutput = new FileOutputStream(publicFilename);
        try {
            pair.publicKey.serialize(new DataOutputStream(publicOutput));
        } finally {
            publicOutput.close();
        }
    }
}
