package transaction;

/**
 * Created by willronchetti on 2/28/17.
 */

import utils.Crypto;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;

/** Signature Class for the Transaction. Serves to provide some flexibility for
 * transaction signatures, that way we can support types other than Pay-to-pubkey.
 * OpCode NONE is defined to be 0x00, and OpCode OP_P2PK, defined to be 0x01,
 * is pay-to-pubkey. These OpCodes will be used to determine how to verify a transaction.
 */
public class RSignature {

    public final static byte NONE = 0x00;
    public final static byte OP_P2PK = 0x01;

    public final static int SCRIPT_SIZE = 32;
    public final static int PUBLIC_KEY_SIZE = 91;
    public final static int SIGNATURE_SIZE = 32;

    private byte opCode;
    private byte[] signature;

    /**
     * Public constructor for the signature class. Sets the opcode to none at the start.
     * If it remains this way, anyone can claim the output.
     */
    public RSignature() {
        opCode = NONE;
        signature = new byte[SIGNATURE_SIZE];
    }

    /**
     * Sets opcode for the transaction signature.
     *
     * @param op is an opcode indicating the signature type. Only P2PK is supported
     *           as of now.
     * @return true if success, assertion failure otherwise.
     */
    public boolean setOpCode(byte op) {
        assert (op == OP_P2PK);
        opCode = op;
        return true;
    }

    /** Signs the input with the provided private key. This takes the public key
     * of who the funds are being transferred to, and signs it with the current
     * coins owners private key. This private key corresponds to the public key
     * in the previous transactions output.
     *
     * @param txbody is the serialized transaction body to be signed.
     * @param PrKey is the private key to be used to sign the input.
     * @return true if successful, throws exception otherwise
     * @throws GeneralSecurityException
     */
    public boolean produceSignature(byte[] txbody, PrivateKey PrKey) throws GeneralSecurityException {
        assert (opCode == OP_P2PK);
        signature = Crypto.sign(txbody, PrKey);
        return true;
    }

    /**
     * Verifies the transaction input signature. The signature takes the hash of the
     * new owner key as input and produces the signature in the signature field.
     *
     * @param txbody is the serialized transaction body which was signed.
     * @param key is the public key used to generate the signature.
     * @return true if the signature verifies, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean verifySignature(byte[] txbody, PublicKey key) throws GeneralSecurityException {
        assert (opCode == OP_P2PK);
        return Crypto.verify(txbody, signature, key);
    }

}
