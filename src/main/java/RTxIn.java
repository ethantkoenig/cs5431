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
    byte[] prevtxid;
    int txidx;
    byte[] pubkeyscript;
    byte[] signature;

    public RTxIn() {
        prevtxid = new byte[32];
        txidx = 0;
        pubkeyscript = new byte[32];
        signature = new byte[32];
    }

//  Enforce TxID size of 32 Bytes.
    void setPrevTxID(byte[] TxID) throws AssertionError {
        assert TxID.length == 32;
        prevtxid = TxID;
    }

    void setTxIndex(int idx) {
        txidx = idx;
    }

//  PKeyScript should be the SHA256 hash of the public key holding the funds
    void setPubkeyScript(byte[] PKeyScript) {
        assert PKeyScript.length == 32;
        pubkeyscript = PKeyScript;
    }

//  Signs pubkeyscript with Private Key passed in
    byte[] produceSignature(PrivateKey PrKey) throws GeneralSecurityException {
        signature = Crypto.sign(pubkeyscript, PrKey);
        return signature;
    }
}
