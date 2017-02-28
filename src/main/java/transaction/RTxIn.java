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
    byte[] newOwnerKey;
    byte[] signature;

    /** Public constructor for TxIn object, sets default fields and allocates memory.
     *
     */
    public RTxIn() {
        prevTxId = new byte[32];
        txIdx = 0;
        newOwnerKey = new byte[91];
        signature = new byte[32];
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

    /** Sets the public key of the new owner.
     *
     * @param PKeyScript is the public key of who the funds will be transferred to.
     */
    public void setPubkeyScript(byte[] PKeyScript) {
        assert PKeyScript.length == 32;
        newOwnerKey = PKeyScript.clone();
    }

    /** Signs the input with the provided private key. This takes the public key
     * of who the funds are being transferred to, and signs it with the current
     * coins owners private key. This private key corresponds to the public key
     * in the previous transactions output.
     *
     * @param PrKey is the
     * @return true if successful, throws exception otherwise
     * @throws GeneralSecurityException
     */
    public boolean produceSignature(PrivateKey PrKey) throws GeneralSecurityException {
        signature = Crypto.sign(newOwnerKey, PrKey);
        return true;
    }
}
