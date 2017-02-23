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
    ByteBuffer prevTxId;
    int txIdx;
    ByteBuffer script;
    ByteBuffer signature;

    public RTxIn() {
        prevTxId = ByteBuffer.allocate(32);
        txIdx = 0;
        script = ByteBuffer.allocate(32);
        signature = ByteBuffer.allocate(32);
    }

//  Enforce TxID size of 32 Bytes.
    void setPrevTxID(ByteBuffer TxID) throws AssertionError {
        assert TxID.capacity() == 32;
        prevTxId = TxID;
    }

    void setTxIndex(int idx) {
        txIdx = idx;
    }

//  PKeyScript should be the SHA256 hash of the public key holding the funds
    void setPubkeyScript(ByteBuffer PKeyScript) {
        assert PKeyScript.capacity() == 32;
        script = PKeyScript;
    }

//  Signs pubkeyscript with Private Key passed in
    byte[] produceSignature(PrivateKey PrKey) throws GeneralSecurityException {
        signature = ByteBuffer.wrap(Crypto.sign(script.array(), PrKey));
        return signature.array();
    }
}
