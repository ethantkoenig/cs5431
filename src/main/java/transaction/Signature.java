package transaction;

import utils.Crypto;
import utils.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Represents a signature for a transaction.
 */
public final class Signature {

    private final byte[] signature;

    private Signature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Sign a transaction body using a private key.
     */
    public static Signature sign(byte[] body, PrivateKey key) throws GeneralSecurityException {
        return new Signature(Crypto.sign(body, key));
    }

    /**
     * Deserialize a signature.
     *
     * @param input input to deserialize
     * @return deserialized signature
     */
    public static Signature deserialize(DataInputStream input) throws IOException {
        int signatureLen = input.readInt();
        byte[] signature = new byte[signatureLen];
        IOUtils.fill(input, signature);
        return new Signature(signature);
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
