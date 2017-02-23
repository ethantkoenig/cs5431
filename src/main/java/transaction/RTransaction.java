package transaction;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

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

    public void setNumTxIn(int numinputs){
        assert numInputs > 0;
        numInputs = numinputs;
        txIn = new RTxIn[numInputs];
    }

//  Insert a new TxIn object in the proper slot. Returns in case that slots
//  are full.
//  XXX: Figure out a better way to handle this case. Throw exception?
    public boolean insertTxIn(ByteBuffer txid, int idx, ByteBuffer pubkey){
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

    public void setNumTxOut(int numoutputs) {
        assert numOutputs > 0;
        numOutputs = numoutputs;
    }

    public boolean insertTxOut(int val, ByteBuffer pubkey) throws GeneralSecurityException {
        if (insertOutputIdx == numInputs - 1) {
            return false;
        }
        txOut[insertOutputIdx] = new RTxOut();
        txOut[insertOutputIdx].setValue(val);
        txOut[insertOutputIdx].setScriptpubkey(pubkey);
        insertOutputIdx++;
        return true;
    }

}
