package transaction;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import utils.Crypto;
import java.security.*;

/**
 * Created by willronchetti on 2/21/17.
 */

/** Class representing the TxID, input index and Script triple.
  * TxID is a hash corresponding to a previous transaction to be referenced
  * in the new transaction. Input idex is the output number of the previous
  * transaction. The script (for now) is the SHA256 hash of the public key
  * owning the funds, which will be signed using the associated Private Key
 */
public class RTxIn {
    byte[] prevTxId;
    int txIdx;
    RSignature signature;

    /** Public constructor for TxIn object, sets default fields and allocates memory.
     *
     */
    public RTxIn() {
        prevTxId = new byte[32];
        txIdx = 0;
        signature = new RSignature();
    }

    /** Sets the TxID to be referenced by this input.
     *
     * @param TxID is the previous transaction hash
     * @throws AssertionError if TxID is not the proper size.
     */
    public void setPrevTxID(byte[] TxID) throws AssertionError {
        assert TxID.length == 32;
        prevTxId = TxID.clone();
    }

    /** Sets the output index to be spent.
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
     * @param script can be none - it is not used at this time
     * @param newkey is the public key of the new owner of the coins.
     */
    public void createSignature(byte op, byte[] newkey, byte[] script) {
        signature = new RSignature();
        signature.setOpCode(op);
        signature.setOwnerKey(newkey);
        signature.setScript(script);
    }

    /**
     * Signs this input.
     *
     * @param key is the private key to be used to sign the transaction.
     * @return true in success, raises exception otherwise.
     * @throws GeneralSecurityException
     */
    public boolean signSignature(PrivateKey key) throws GeneralSecurityException {
        return signature.produceSignature(key);
    }

    /**
     * Verifies the signature on this input.
     *
     * @param key is the corresponding public key of who signed the input.
     * @return true in success, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean verifySignature(PublicKey key) throws GeneralSecurityException {
        return signature.verifySignature(key);
    }


}
