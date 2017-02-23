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
    private int insertinputidx;
    int numinputs;
    RTxIn[] txin;
    private int insertoutputidx;
    int numoutputs;
    RTxOut[] txout;

    public RTransaction() {
        insertinputidx = 0;
        numinputs = 0;
        txin = new RTxIn[0];
        insertoutputidx = 0;
        numoutputs = 0;
    }

    public void setNumTxIn(int numInputs){
        assert numInputs > 0;
        numinputs = numInputs;
        txin = new RTxIn[numInputs];
    }

//  Insert a new TxIn object in the proper slot. Returns in case that slots
//  are full.
//  XXX: Figure out a better way to handle this case. Throw exception?
    public void insertTxIn(ByteBuffer txid, int idx, ByteBuffer pubkey){
        if (insertinputidx == numinputs - 1) {
            return;
        }
        txin[insertinputidx] = new RTxIn();
        txin[insertinputidx].prevtxid = txid;
        txin[insertinputidx].txidx = idx;
        txin[insertinputidx].script = pubkey;
        insertinputidx++;
    }

    public void setNumTxOut(int numOutputs) {
        assert numOutputs > 0;
        numoutputs = numOutputs;
    }

    public void insertTxOut(int val, ByteBuffer pubkey) throws GeneralSecurityException {
        if (insertoutputidx == numinputs - 1) {
            return;
        }
        txout[insertoutputidx] = new RTxOut();
        txout[insertoutputidx].setValue(val);
        txout[insertoutputidx].setScriptpubkey(pubkey);
        insertoutputidx++;
    }

}
