package transaction;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

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

    public RTransaction() {
        insertInputIdx = 0;
        numInputs = 0;
        txIn = new RTxIn[0];
        insertOutputIdx = 0;
        numOutputs = 0;
    }

    private void setNumTxIn(int numinputs){
        assert numInputs > 0;
        numInputs = numinputs;
        txIn = new RTxIn[numInputs];
    }

    private boolean insertTxIn(ByteBuffer txid, int idx, ByteBuffer pubkey){
        if (insertInputIdx == numInputs - 1) {
            return false;
        }
        txIn[insertInputIdx] = new RTxIn();
        txIn[insertInputIdx].prevTxId = txid;
        txIn[insertInputIdx].txIdx = idx;
        txIn[insertInputIdx].script = pubkey;
        insertInputIdx++;
        return true;
    }

    private void setNumTxOut(int numoutputs) {
        assert numOutputs > 0;
        numOutputs = numoutputs;
    }

    private boolean insertTxOut(int val, ByteBuffer pubkey) throws GeneralSecurityException {
        if (insertOutputIdx == numInputs - 1) {
            return false;
        }
        txOut[insertOutputIdx] = new RTxOut();
        txOut[insertOutputIdx].setValue(val);
        txOut[insertOutputIdx].setScriptpubkey(pubkey);
        insertOutputIdx++;
        return true;
    }

//  Takes in a hash as a string and returns a ByteBuffer containing the Byte representation
//  of the string. Hash should be either a TxId or a Public Key.
    public ByteBuffer convertHashFromString(String hash) {
        return ByteBuffer.wrap(hash.getBytes(Charset.forName("UTF8")));
    }

//  Public method for adding TxIn's to the transaction
    public boolean addTxIns(int numinputs, ByteBuffer[] hashes, int[] idx, ByteBuffer pubkey) {
        setNumTxIn(numinputs);
        for (int i = 0; i < numinputs; i++) {
            insertTxIn(hashes[i], idx[i], pubkey);
        }
        return true;
    }

//  Signs the users inputs to the transaction
    public boolean signInputs(PrivateKey key) throws GeneralSecurityException {
        assert numInputs > 0;
        for (int i = 0; i < numInputs; i++) {
            txIn[i].produceSignature(key);
        }
        return true;
    }

//  Public method for adding TxOut's to the transaction
    public boolean addTxOuts(int numoutputs, int[] amts, ByteBuffer pubkeyscript) throws GeneralSecurityException {
        setNumTxOut(numoutputs);
        for (int i = 0; i < numOutputs; i++) {
            insertTxOut(amts[i], pubkeyscript);
        }
        return true;
    }

}
