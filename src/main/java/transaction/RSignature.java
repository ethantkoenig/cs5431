package transaction;

import utils.Crypto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Represents a signature for a transaction.
 */
public class RSignature {

    private final byte[] signature;

    private RSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Sign a transaction body using a private key.
     */
    public static RSignature sign(byte[] body, PrivateKey key) throws GeneralSecurityException {
        return new RSignature(Crypto.sign(body, key));
    }

    /**
     * Deserialize a signature.
     *
     * @param byteBuffer bytes to deserialize
     * @return deserialized signature
     */
    public static RSignature deserialize(ByteBuffer byteBuffer) {
        int signatureLen = byteBuffer.getInt();
        byte[] signature = new byte[signatureLen];
        byteBuffer.get(signature);
        return new RSignature(signature);
    }

    /**
     * Serialize a signature
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(signature.length);
        outputStream.write(signature);
    }

    /**
     * Verifies the transaction input signature. The signature takes the hash of the
     * new owner key as input and produces the signature in the signature field.
     *
     * @param body is the serialized transaction body which was signed.
     * @param key  is the public key used to generate the signature.
     * @return true if the signature verifies, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean verify(byte[] body, PublicKey key) throws GeneralSecurityException {
        return Crypto.verify(body, signature, key);
    }
}
