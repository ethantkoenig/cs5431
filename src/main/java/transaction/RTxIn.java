package transaction;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;


/**
 * Class representing the TxID, input index and Script triple.
 * TxID is a hash corresponding to a previous transaction to be referenced
 * in the new transaction. Input idex is the output number of the previous
 * transaction. The script (for now) is the SHA256 hash of the public key
 * owning the funds, which will be signed using the associated Private Key
 */
public class RTxIn {

    public static final int HASH_SIZE = 32;

    private byte[] prevTxId;
    private int txIdx;
    private RSignature signature;

    /**
     * Public constructor for TxIn object, sets default fields and allocates memory.
     */
    public RTxIn() {
        prevTxId = new byte[HASH_SIZE];
        txIdx = 0;
        signature = new RSignature();
    }

    /**
     * Sets the TxID to be referenced by this input.
     *
     * @param TxID is the previous transaction hash
     * @throws AssertionError if TxID is not the proper size.
     */
    public void setPrevTxID(byte[] TxID) throws AssertionError {
        assert (TxID.length == HASH_SIZE);
        prevTxId = TxID.clone();
    }

    /**
     * Sets the output index to be spent.
     *
     * @param idx is the corresponding output index in a previous transaction.
     */
    public void setTxIndex(int idx) {
        txIdx = idx;
    }

    /**
     * Method for creating the signature part of the transaction.
     *
     * @param op is an opcode corresponding to the type of script. Only pay to pubkey
     *           is supported at this time.
     */
    public void createSignature(byte op) {
        signature = new RSignature();
        signature.setOpCode(op);
    }

    /**
     * Signs this input.
     *
     * @param txbody is the serialized transaction body to be signed.
     * @param key    is the private key to be used to sign the transaction.
     * @return true in success, raises exception otherwise.
     * @throws GeneralSecurityException
     */
    public boolean signSignature(byte[] txbody, PrivateKey key) throws GeneralSecurityException {
        return signature.produceSignature(txbody, key);
    }

    /**
     * Verifies the signature on this input.
     *
     * @param txbody is the serialized transaction body that was signed.
     * @param key    is the corresponding public key of who signed the input.
     * @return true in success, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean verifySignature(byte[] txbody, PublicKey key) throws GeneralSecurityException {
        return signature.verifySignature(txbody, key);
    }

}
