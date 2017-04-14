package crypto;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import utils.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.KeySpec;

/**
 * Various crypto-related functions
 */
public final class Crypto {
    static final ECNamedCurveParameterSpec SPEC =
            ECNamedCurveTable.getParameterSpec("P-256");
    private static final ECDomainParameters PARAMETERS = new ECDomainParameters(
            SPEC.getCurve(),
            SPEC.getG(),
            SPEC.getN()
    );

    private static boolean initialized = false;

    // Disallow instances of this class
    private Crypto() {
    }

    public static void init() {
        if (!initialized) {
            Security.addProvider(new BouncyCastleProvider());
            initialized = true;
        }
    }

    public static ECDSAKeyPair signatureKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(SPEC, Config.secureRandom());
        KeyPair pair = keyGen.generateKeyPair();

        ECPrivateKey ecPrivateKey = (ECPrivateKey) pair.getPrivate();
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(ecPrivateKey.getD());

        ECPublicKey ecPublicKey = (ECPublicKey) pair.getPublic();
        ECDSAPublicKey publicKey = new ECDSAPublicKey(ecPublicKey.getQ());
        return new ECDSAKeyPair(privateKey, publicKey);
    }

    public static ECDSASignature sign(byte[] toSign, ECDSAPrivateKey key) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, new ECPrivateKeyParameters(key.d, PARAMETERS));
        BigInteger[] signatureIntegers = signer.generateSignature(sha256(toSign));
        if (signatureIntegers.length != 2) {
            throw new AssertionError("Invalid ECDSA signature");
        }
        return new ECDSASignature(signatureIntegers[0], signatureIntegers[1]);
    }

    public static boolean verify(byte[] content, ECDSASignature signature, ECDSAPublicKey key) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, new ECPublicKeyParameters(key.point, PARAMETERS));
        return signer.verifySignature(sha256(content), signature.r, signature.s);
    }

    public static ECDSAPublicKey loadPublicKey(String filename)
            throws DeserializationException, IOException {
        InputStream inputStream = new FileInputStream(filename);
        return ECDSAPublicKey.DESERIALIZER.deserialize(
                new DataInputStream(inputStream)
        );
    }

    public static ECDSAPrivateKey loadPrivateKey(String filename)
            throws DeserializationException, IOException {
        InputStream inputStream = new FileInputStream(filename);
        return ECDSAPrivateKey.DESERIALIZER.deserialize(
                new DataInputStream(inputStream)
        );
    }

    public static byte[] sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(content);
        } catch (NoSuchAlgorithmException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public static byte[] pbkdf2(String content, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(content.toCharArray(), salt, Config.pbkdf2Cost(), 2048);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        Config.secureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hashAndSalt(String password, byte[] salt)
            throws Exception {
        return pbkdf2(password, salt);
    }

}
