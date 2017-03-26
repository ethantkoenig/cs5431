package utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Various crypto-related functions
 */
public class Crypto {
    public static final int PRIVATE_KEY_LEN_IN_BYTES = 150;
    public static final int PUBLIC_KEY_LEN_IN_BYTES = 91;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            Security.addProvider(new BouncyCastleProvider());
            initialized = true;
        }
    }

    public static KeyPair signatureKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
        keyGen.initialize(ecSpec, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    public static PublicKey deserializePublicKey(InputStream input)
            throws GeneralSecurityException, IOException {
        byte[] array = new byte[PUBLIC_KEY_LEN_IN_BYTES];
        IOUtils.fill(input, array);
        return parsePublicKey(array);
    }

    public static PublicKey parsePublicKey(byte[] bytes) throws GeneralSecurityException {
        return KeyFactory.getInstance("ECDSA", "BC").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static PrivateKey parsePrivateKey(byte[] bytes) throws GeneralSecurityException {
        return KeyFactory.getInstance("ECDSA", "BC").generatePrivate(new PKCS8EncodedKeySpec(bytes));
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

    public static byte[] pbkdf2(String content, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(content.toCharArray(), salt, Config.PBKDF2_COST.get(), 2048);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    public static PublicKey loadPublicKey(String filename)
            throws GeneralSecurityException, IOException {
        InputStream inputStream = new FileInputStream(filename);
        byte[] keyBytes = new byte[Crypto.PUBLIC_KEY_LEN_IN_BYTES];
        IOUtils.fill(inputStream, keyBytes);
        return parsePublicKey(keyBytes);
    }

    public static PrivateKey loadPrivateKey(String filename)
            throws GeneralSecurityException, IOException {
        InputStream inputStream = new FileInputStream(filename);
        byte[] keyBytes = new byte[Crypto.PRIVATE_KEY_LEN_IN_BYTES];
        IOUtils.fill(inputStream, keyBytes);
        return parsePrivateKey(keyBytes);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hashAndSalt(String password, byte[] salt)
            throws Exception {
        return pbkdf2(password, salt);
    }

}
