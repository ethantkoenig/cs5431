import java.nio.ByteBuffer;

/**
 * Created by willronchetti on 2/21/17.
 */
public class RTransaction {
    RTxIn txin;

    public void setTxIn(byte[] txid, int idx, byte[] pubkey){
        txin = new RTxIn();
        txin.prevtxid = txid;
        txin.txidx = idx;
        txin.pubkeyscript = pubkey;
    }

    public RTxIn getTxIn() {
        return txin;
    }
}
