import java.nio.ByteBuffer;

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
    ByteBuffer prevtxid;
    int txidx;
    ByteBuffer pubkeyscript;

    public RTxIn() {
        prevtxid = ByteBuffer.allocate(32);
        txidx = 0;
        pubkeyscript = ByteBuffer.allocate(32);
    }

//  Enforce TxID size of 32 Bytes.
    void setPrevTxID(ByteBuffer TxID) throws AssertionError {
        assert TxID.capacity() == 32;
        prevtxid = TxID;
    }

    void setTxIndex(int idx) {
        txidx = idx;
    }

//  PKeyScript should be the SHA256 hash of the public key holding the funds
    void setPubkeyScript(ByteBuffer PKeyScript) {
        assert PKeyScript.capacity() == 32;
        pubkeyscript = PKeyScript;
    }

//  XXX: Finish
    boolean produceSignature(ByteBuffer PrivateKey) {
        return false;
    }


}
