package cli;


import utils.Crypto;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class GenerateKey {

    public static void generateKey(String privateFilename, String publicFilename)
            throws GeneralSecurityException, IOException {
        KeyPair pair = Crypto.signatureKeyPair();

        OutputStream privateOuput = new FileOutputStream(privateFilename);
        try {
            privateOuput.write(pair.getPrivate().getEncoded());
        } finally {
            privateOuput.close();
        }

        OutputStream publicOutput = new FileOutputStream(publicFilename);
        try {
            publicOutput.write(pair.getPublic().getEncoded());
        } finally {
            publicOutput.close();
        }
    }
}