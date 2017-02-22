package utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

/* Various crypto-related functions */
public class Crypto {
    public static void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair signatureKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
        keyGen.initialize(ecSpec, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    public static byte[] sign(byte[] toSign, PrivateKey key) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("ECDSA");
        signature.initSign(key, new SecureRandom());
        signature.update(toSign);
        return signature.sign();
    }

    public static boolean verify(byte[] content, byte[] signed, PublicKey key)
            throws GeneralSecurityException {
        Signature signature = Signature.getInstance("ECDSA");
        signature.initVerify(key);
        signature.update(content);
        return signature.verify(signed);
    }

    public static byte[] sha256(byte[] content) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(content);
    }
}
