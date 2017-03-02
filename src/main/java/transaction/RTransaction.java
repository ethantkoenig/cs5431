package transaction;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Main transaction class. Keeps track of several pieces of information key to the
 * transaction, such as the number of inputs, and outputs, the inputs and outputs
 * themselves, and some additional bookkeeping for proper insertion of input and
 * output classes
 * <p>
 * insertInput/OutputIdx is a private identifier for insertion. Should probably use
 * an Arraylist to support dynamic allocation.
 * numInputs/Outputs is the number of inputs/outputs the transaction contains.
 * txIn is an array of inputs to the transaction.
 * txOut is an array of the outputs to the transaction.
 */
public class RTransaction {
    private int insertInputIdx;
    public int numInputs;
    public RTxIn[] txIn;
    private int insertOutputIdx;
    public int numOutputs;
    public RTxOut[] txOut;

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

    private void setNumTxIn(int num) {
        assert (num > 0);
        numInputs = num;
        txIn = new RTxIn[numInputs];
    }

    private boolean insertTxIn(byte[] txid, int idx, PublicKey pubkey, PublicKey mykey) throws GeneralSecurityException {
        if (insertInputIdx == numInputs) {
            return false;
        }
        txIn[insertInputIdx] = new RTxIn();
        txIn[insertInputIdx].setPrevTxID(txid);
        txIn[insertInputIdx].setTxIndex(idx);
        txIn[insertInputIdx].createSignature(RSignature.OP_P2PK);
        insertInputIdx++;
        return true;
    }

    private void setNumTxOut(int num) {
        assert (num > 0);
        numOutputs = num;
        txOut = new RTxOut[numOutputs];
    }

    private boolean insertTxOut(long val, PublicKey pubkey) throws GeneralSecurityException {
        if (insertOutputIdx == numOutputs) {
            return false;
        }
        txOut[insertOutputIdx] = new RTxOut();
        txOut[insertOutputIdx].setValue(val);
        txOut[insertOutputIdx].setScriptpubkey(pubkey.getEncoded());
        insertOutputIdx++;
        return true;
    }

    /**
     * Serializes the transaction body to be signed.
     *
     * @return the serialized transaction body as a byte array to be signed.
     */
    private byte[] serializeBody() {
        return new byte[0];
    }

    /**
     * Public method for adding TxIn's to the transaction.
     *
     * @param num     is the number of inputs to be added
     * @param hashes  is a 2D-array of TxID's to be referenced
     * @param idx     is a list of the corresponding indexes of the previous transaction.
     *                Note: hashes[i][] and idx[i] should go together in that one input is the txid
     *                stored in hashes[i] and its location in the previous transaction is idx[i]
     * @param pubkeys is the new owners public key, which should correspond to a previous
     *                transaction. The signature will be the hash of this public key signed by your
     *                private key. It is also a 2D-array of bytearrays.
     * @param mykey   is your public key.
     * @return true in success, false otherwise
     * @throws GeneralSecurityException in the case of hashing failure.
     */
    public boolean addTxIns(int num, byte[][] hashes, int[] idx, PublicKey[] pubkeys, PublicKey mykey) throws GeneralSecurityException {
        setNumTxIn(num);
        boolean result = true;
        for (int i = 0; i < num; i++) {
            result = result && insertTxIn(hashes[i], idx[i], pubkeys[i], mykey);
        }
        return result;
    }

    /**
     * Signs the users inputs to the transaction
     *
     * @param key is the Private key to be used to sign the input.
     * @return true in success, will throw exception otherwise.
     * @throws GeneralSecurityException
     */
    public boolean signInputs(PrivateKey key) throws GeneralSecurityException {
        assert (numInputs > 0);
        byte[] txSig = serializeBody();
        for (int i = 0; i < numInputs; i++) {
            txIn[i].signSignature(txSig, key);
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
        byte[] txSig = serializeBody();
        for (int i = 0; i < numInputs; i++) {
            result = result && txIn[i].verifySignature(txSig, keys[i]);
        }
        return result;
    }

    /**
     * Public method for adding TxOut's to the transaction
     *
     * @param num           is the number of outputs to be added.
     * @param amts          is an array corresponding to the amounts.
     * @param pubkeyscripts is a 2D array of output addresses.
     * @return true in success, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean addTxOuts(int num, long[] amts, PublicKey[] pubkeyscripts) throws GeneralSecurityException {
        setNumTxOut(num);
        boolean result = true;
        for (int i = 0; i < num; i++) {
            result = result && insertTxOut(amts[i], pubkeyscripts[i]);
        }
        return result;
    }

}
