package transaction;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

import utils.Crypto;

/**
 * Created by willronchetti on 2/21/17.
 */

/**
 * Main transaction class. Keeps track of several pieces of information key to the
 * transaction, such as the number of inputs, and outputs, the inputs and outputs
 * themselves, and some additional bookkeeping for proper insertion of input and
 * output classes
 */
public class RTransaction {
    private int insertInputIdx;
    int numInputs;
    RTxIn[] txIn;
    private int insertOutputIdx;
    int numOutputs;
    RTxOut[] txOut;

    /**
     * Constructor function for the Transaction class. Sets default values, users should
     * use the methods provided to populate the transaction.
     */
    public RTransaction() {
        insertInputIdx = 0;
        numInputs = 0;
        insertOutputIdx = 0;
        numOutputs = 0;
    }

    private void setNumTxIn(int numinputs){
        assert numinputs > 0;
        numInputs = numinputs;
        txIn = new RTxIn[numInputs + 1];
    }

    private boolean insertTxIn(byte[] txid, int idx, PublicKey pubkey) throws GeneralSecurityException {
        if (insertInputIdx == numInputs) {
            return false;
        }
        txIn[insertInputIdx] = new RTxIn();
        txIn[insertInputIdx].setPrevTxID(txid);
        txIn[insertInputIdx].setTxIndex(idx);
        txIn[insertInputIdx].createSignature(RSignature.OP_P2PK, pubkey.getEncoded(), new byte[0]);
        insertInputIdx++;
        return true;
    }

    private void setNumTxOut(int numoutputs) {
        assert numoutputs > 0;
        numOutputs = numoutputs;
        txOut = new RTxOut[numOutputs + 1];
    }

    private boolean insertTxOut(int val, PublicKey pubkey) throws GeneralSecurityException {
        if (insertOutputIdx == numInputs) {
            return false;
        }
        txOut[insertOutputIdx] = new RTxOut();
        txOut[insertOutputIdx].setValue(val);
        txOut[insertOutputIdx].setScriptpubkey(pubkey.getEncoded());
        insertOutputIdx++;
        return true;
    }

    /** Takes in a hash as a string and returns a ByteBuffer containing the Byte representation
     *  of the string. Hash should be either a TxId or a Public Key.
     *
     *  @param hash is a string representation of the previous transactions hash
     *  @return A byte array containing the hash encoded from UTF8
     */
     public byte[] convertHashFromString(String hash) {
        return hash.getBytes(Charset.forName("UTF8"));
    }

    /** Public method for adding TxIn's to the transaction.
     *
     * @param numinputs is the number of inputs to be added
     * @param hashes is a 2D-array of TxID's to be referenced
     * @param idx is a list of the corresponding indexes of the previous transaction.
     *    Note: hashes[i][] and idx[i] should go together in that one input is the txid
     *    stored in hashes[i] and its location in the previous transaction is idx[i]
     * @param pubkey is the new owners public key, which should correspond to a previous
     * transaction. The signature will be the hash of this public key signed by your
     * private key. It is also a 2D-array of bytearrays.
     *
     * @throws GeneralSecurityException in the case of hashing failure.
     *
     * @return true in success, false otherwise
     */
    public boolean addTxIns(int numinputs, byte[][] hashes, int[] idx, PublicKey[] pubkey) throws GeneralSecurityException{
        setNumTxIn(numinputs);
        boolean result = true;
        for (int i = 0; i < numinputs; i++) {
            result = result && insertTxIn(hashes[i], idx[i], pubkey[i]);
        }
        return result;
    }

    /**  Signs the users inputs to the transaction
     *
     * @param key is the Private key to be used to sign the input.
     *
     * @return true in success, will throw exception otherwise.
     * @throws GeneralSecurityException
     */
    public boolean signInputs(PrivateKey key) throws GeneralSecurityException {
        assert numInputs > 0;
        for (int i = 0; i < numInputs; i++) {
            txIn[i].signSignature(key);
        }
        return true;
    }

    /**
     * Verfies all signatures on the transaction given an array of corresponding
     * public keys.
     *
     * @param keys is an array of public keys corresponding to which private key
     *             signed each input.
     * @return true if successful, false otherwise
     * @throws GeneralSecurityException
     */
    public boolean verifySig(PublicKey[] keys) throws GeneralSecurityException {
        boolean result = true;
        for (int i = 0; i < numInputs; i++) {
            result = result && txIn[i].verifySignature(keys[i]);
        }
        return result;
    }

    /** Public method for adding TxOut's to the transaction
     *
     * @param numoutputs is the number of outputs to be added.
     * @param amts is an array corresponding to the amounts.
     * @param pubkeyscripts is a 2D array of output addresses.
     *
     * @return true in success, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean addTxOuts(int numoutputs, int[] amts, PublicKey[] pubkeyscripts) throws GeneralSecurityException {
        setNumTxOut(numoutputs);
        boolean result = true;
        for (int i = 0; i < numOutputs; i++) {
            result = result && insertTxOut(amts[i], pubkeyscripts[i]);
        }
        return result;
    }

}
