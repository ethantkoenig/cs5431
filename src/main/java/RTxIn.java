import java.nio.ByteBuffer;

/**
 * Created by willronchetti on 2/21/17.
 */

// Class representing the TxID, input index and Script triple.
public class RTxIn {
    ByteBuffer PrevTxID;
    int TxIndex;
    ByteBuffer PubkeyScript;

    public RTxIn() {
        PrevTxID = ByteBuffer.allocate(32);
        TxIndex = 0;
        PubkeyScript = ByteBuffer.allocate(91);
    }

//  Enforce TxID size of 32 Bytes.
    void setPrevTxID(ByteBuffer TxID) throws AssertionError {
        assert TxID.capacity() == 32;
        PrevTxID = TxID;
    }

    void setTxIndex(int idx) {
        TxIndex = idx;
    }

//  Enforce public key size of 91 bytes
    void setPubkeyScript(ByteBuffer PKey) {
        assert PKey.capacity() == 91;
        PubkeyScript = PKey;
    }

//  XXX: Finish
    boolean produceSignature(ByteBuffer PrivateKey) {
        return false;
    }


}
