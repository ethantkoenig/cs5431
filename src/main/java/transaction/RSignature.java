package transaction;

/**
 * Created by willronchetti on 2/28/17.
 */

import utils.Crypto;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/** Signature Class for the Transaction. Serves to provide some flexibility for
 * transaction signatures, that way we can support types other than Pay-to-pubkey.
 *
 */
public class RSignature {

    public final static byte NONE = 0x00;
    public final static byte OP_P2PK = 0x01;

    byte opCode;
    byte[] script;
    byte[] newOwnerKey;
    byte[] signature;

    /**
     * Public constructor for the signature class. Sets the opcode to none at the start.
     * If it remains this way, anyone can claim the output.
     */
    public RSignature() {
        opCode = NONE;
        script = new byte[32];
        newOwnerKey = new byte[91];
        signature = new byte[32];
    }

    /**
     * Sets opcode for the transaction signature.
     *
     * @param op is an opcode indicating the signature type. Only P2PK is supported
     *           as of now.
     * @return true if success, assertion failure otherwise.
     */
    public boolean setOpCode(byte op) {
        assert op == OP_P2PK;
        opCode = op;
        return true;
    }

    /**
     * Method to set the redeem script. This will not be used at this time but is necessary
     * to implement other transaction types.
     *
     * @param redeemscript is a 32 byte script
     */
    public void setScript(byte[] redeemscript) {
        script = redeemscript.clone();
    }

    /** Sets the public key of the new owner.
     *
     * @param ownerkey is the public key of who the funds will be transferred to.
     */
    public void setOwnerKey(byte[] ownerkey) {
        assert ownerkey.length == 91;
        newOwnerKey = ownerkey.clone();
    }

    /** Signs the input with the provided private key. This takes the public key
     * of who the funds are being transferred to, and signs it with the current
     * coins owners private key. This private key corresponds to the public key
     * in the previous transactions output.
     *
     * @param PrKey is the private key to be used to sign the input.
     * @return true if successful, throws exception otherwise
     * @throws GeneralSecurityException
     */
    public boolean produceSignature(PrivateKey PrKey) throws GeneralSecurityException {
        assert opCode == OP_P2PK;
        signature = Crypto.sign(Crypto.sha256(newOwnerKey), PrKey);
        return true;
    }

    /**
     * Verifies the transaction input signature. The signature takes the hash of the
     * new owner key as input and produces the signature in the signature field.
     *
     * @param key is the public key used to generate the signature.
     * @return true if the signature verifies, false otherwise.
     * @throws GeneralSecurityException
     */
    public boolean verifySignature(PublicKey key) throws GeneralSecurityException {
        assert opCode == OP_P2PK;
        return Crypto.verify(Crypto.sha256(newOwnerKey), signature, key);
    }


}
