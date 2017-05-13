package crypto;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import utils.Config;
import utils.DeserializationException;
import utils.ShaTwoFiftySix;

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
@Singleton
public final class Crypto {
    static final ECNamedCurveParameterSpec SPEC =
            ECNamedCurveTable.getParameterSpec("P-256");
    private static final ECDomainParameters PARAMETERS = new ECDomainParameters(
            SPEC.getCurve(),
            SPEC.getG(),
            SPEC.getN()
    );
    public static final int ECDSA_ORDER_IN_BYTES = 32;
    public static final int SALT_LEN_IN_BYTES = 16;

    private SecureRandom secureRandom;

    @Inject
    public Crypto(SecureRandom secureRandom) {
        Security.addProvider(new BouncyCastleProvider());
        this.secureRandom = secureRandom;
    }

    public ECDSAKeyPair signatureKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(SPEC, secureRandom);
        KeyPair pair = keyGen.generateKeyPair();

        ECPrivateKey ecPrivateKey = (ECPrivateKey) pair.getPrivate();
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(ecPrivateKey.getD());

        ECPublicKey ecPublicKey = (ECPublicKey) pair.getPublic();
        ECDSAPublicKey publicKey = new ECDSAPublicKey(ecPublicKey.getQ());
        return new ECDSAKeyPair(privateKey, publicKey);
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LEN_IN_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    public String nextGUID() {
        return new BigInteger(130, secureRandom).toString(32);
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
        return verify(ShaTwoFiftySix.hashOf(content), signature, key);
    }

    public static boolean verify(ShaTwoFiftySix contentHash, ECDSASignature signature, ECDSAPublicKey key) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, new ECPublicKeyParameters(key.point, PARAMETERS));
        return signer.verifySignature(contentHash.copyOfHash(), signature.r, signature.s);
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

    public static byte[] pbkdf2(String content, byte[] salt) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(content.toCharArray(), salt, Config.pbkdf2Cost(), 2048);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    public static byte[] hashAndSalt(String password, byte[] salt)
            throws GeneralSecurityException {
        return pbkdf2(password, salt);
    }

}
