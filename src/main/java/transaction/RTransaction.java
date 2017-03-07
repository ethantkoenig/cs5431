package transaction;


import block.UnspentTransactions;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Logger;

/**
 * Main transaction class.
 * Contains an array of inputs, outputs and signatures.
 */
public class RTransaction {
    private final static Logger LOGGER = Logger.getLogger(Logger.class.getName());

    private final RTxIn[] txIn;
    private final RTxOut[] txOut;
    private final RSignature[] signatures;

    public final int numInputs;
    public final int numOutputs;

    private RTransaction(RTxIn[] txIn, RTxOut[] txOut, RSignature[] signatures) {
        this.txIn = txIn;
        this.txOut = txOut;
        this.signatures = signatures;
        numInputs = txIn.length;
        numOutputs = txOut.length;
    }

    private static RTransaction createAndSign(RTxIn[] inputs, RTxOut[] outputs, PrivateKey[] keys)
            throws IOException, GeneralSecurityException {
        if (inputs.length != keys.length) {
            throw new IllegalStateException();
        }
        RTransaction txn = new RTransaction(inputs, outputs, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        txn.serialize(new DataOutputStream(outputStream));
        byte[] body = outputStream.toByteArray();

        RSignature[] signatures = new RSignature[keys.length];
        for (int i = 0; i < keys.length; i++) {
            signatures[i] = RSignature.sign(body, keys[i]);
        }

        return new RTransaction(inputs, outputs, signatures);
    }

    public static RTransaction deserialize(ByteBuffer input) throws GeneralSecurityException {
        final int numInputs = input.getInt();
        RTxIn[] inputs = new RTxIn[numInputs];
        for (int i = 0; i < numInputs; i++) {
            inputs[i] = RTxIn.deserialize(input);
        }

        final int numOutputs = input.getInt();
        RTxOut[] outputs = new RTxOut[numOutputs];
        for (int i = 0; i < numOutputs; i++) {
            outputs[i] = RTxOut.deserialize(input);
        }
        RSignature[] signatures = new RSignature[numInputs];
        for (int i = 0; i < numInputs; i++) {
            signatures[i] = RSignature.deserialize(input);
        }
        return new RTransaction(inputs, outputs, signatures);
    }

    /**
     * Serialize this transaction (excluding signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(txIn.length);
        for (RTxIn input : txIn) {
            input.serialize(outputStream);
        }

        outputStream.writeInt(txOut.length);
        for (RTxOut output : txOut) {
            output.serialize(outputStream);
        }
    }

    /**
     * Serialize this transaction (including signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serializeWithSignatures(DataOutputStream outputStream) throws IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot fully serialize unsigned transaction");
        }
        serialize(outputStream);
        for (RSignature signature : signatures) {
            signature.serialize(outputStream);
        }
    }

    /**
     * Returns the input at position 'index' in the transaction.
     *
     * @param index is the input number to be returned.
     * @return an RTxIn object of this transaction at the requested index.
     */
    public RTxIn getInput(int index) {
        if (index < 0 || index >= txIn.length) {
            String msg = String.format("Invalid index %d", index);
            throw new IllegalArgumentException(msg);
        }
        return txIn[index];
    }

    /**
     * Returns the output at position 'index' in the transaction.
     * @param index is the output number to be returned.
     * @return an RTxOut object of this transaction at the requested index.
     */
    public RTxOut getOutput(int index) {
        if (index < 0 || index >= txOut.length) {
            String msg = String.format("Invalid index %d", index);
            throw new IllegalArgumentException(msg);
        }
        return txOut[index];
    }

    /**
     * Verify the signature corresponding to the {@code inputIndex}^th input.
     *
     * @param inputIndex index of input to verify
     * @param key        public key to use for verification, should match the public key of the output
     *                   corresponding to the relevant input
     * @return whether the signature was verified
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public boolean verifySignature(int inputIndex, PublicKey key)
            throws GeneralSecurityException, IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot verify unsigned transaction");
        } else if (inputIndex < 0 || inputIndex >= signatures.length) {
            String msg = String.format("Invalid index: %d, expected to be in range [0, %d)",
                    inputIndex, txIn.length);
            throw new IllegalArgumentException(msg);
        }

        // TODO: eventually find a way to not re-serialize every time
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serialize(new DataOutputStream(outputStream));
        return signatures[inputIndex].verify(outputStream.toByteArray(), key);
    }

    /**
     * Verifies {@code this RTransaction} with respect to {@code unspentOutputs}.
     * <p>
     * This will check that all signatures are valid and that there are no double spends.
     *
     * @param unspentOutputs A {@code Map} containing all of the unspent outputs that {@code this} may refer to. Entries
     *                       that are spent by {@code this RTransaction} will be removed. If verification fails, there
     *                       are no guarantees as to the state of this {@code Map}.
     * @return Whether this {@code RTransaction} was successfully verified.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public boolean verify(UnspentTransactions unspentOutputs)
            throws GeneralSecurityException, IOException {
        long inputsum = 0;
        long outputsum = 0;
        for (int i = 0; i < txIn.length; ++i) {
            RTxIn in = txIn[i];
            if (!unspentOutputs.contains(in.previousTxn, in.txIdx)) {
                LOGGER.warning("Invalid input: " + in.previousTxn + "," + in.txIdx);
                return false;
            }
            RTxOut out = unspentOutputs.remove(in.previousTxn, in.txIdx);
            inputsum += out.value;
            if (!verifySignature(i, out.ownerPubKey)) {
                LOGGER.warning("Invalid signature: " + out.ownerPubKey + "," + signatures[i]);
                return false;
            }
        }
        for (int j = 0; j < txOut.length; j++) {
            RTxOut out = txOut[j];
            outputsum += out.value;
            unspentOutputs.put(getShaTwoFiftySix(), j, out);
        }
        if (outputsum > inputsum) {
            return false;
        }
        return true;
    }

    /**
     * "Rollbacks" this transaction, by updating {@code unspentTransactions} to
     * reflect the state before this transaction was applied.
     *
     * @param unspentTransactions map of unspent transactions to update
     * @param lookup way to lookup previous transactions
     * @return whether the rollback was successful
     */
    public boolean rollback(UnspentTransactions unspentTransactions, TransactionLookup lookup) {
        ShaTwoFiftySix hash = getShaTwoFiftySix();
        for (int i = 0; i < txOut.length; i++) {
            if (!unspentTransactions.contains(hash, i)) {
                return false;
            }
            unspentTransactions.remove(hash, i);
        }
        for (RTxIn input: txIn) {
            if (unspentTransactions.contains(input.previousTxn, input.txIdx)) {
                return false;
            }

            Optional<RTransaction> opt = lookup.lookup(input.previousTxn);
            if (!opt.isPresent()) {
                return false;
            }

            RTxOut output = opt.get().getOutput(input.txIdx);
            unspentTransactions.put(input.previousTxn, input.txIdx, output);
        }
        return true;
    }

    /**
     * @return The SHA-256 hash of the serialization of {@code this}
     */
    public ShaTwoFiftySix getShaTwoFiftySix() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outputStream));
            return ShaTwoFiftySix.hashOf(outputStream.toByteArray());
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RTransaction)) {
            return false;
        }
        RTransaction other = (RTransaction) o;
        return Arrays.equals(txIn, other.txIn) && Arrays.equals(txOut, other.txOut);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tx: " + ByteUtil.bytesToHexString(getShaTwoFiftySix().copyOfHash()) + "\n");
        builder.append("----TxIn----\n");
        for (int i = 0; i < txIn.length; ++i) {
            builder.append("prevId: " + ByteUtil.bytesToHexString(txIn[i].previousTxn.copyOfHash()));
            builder.append(", txIdx: " + txIn[i].txIdx + "\n");
        }
        builder.append("----TxOut----\n");
        for (int i = 0; i < txOut.length; ++i) {
            builder.append("value: " + txOut[i].value);
            builder.append(", toKey: " + ByteUtil.bytesToHexString(txOut[i].ownerPubKey.getEncoded()) + "\n");
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] { txIn, txOut });
    }

    public static class Builder {
        List<RTxIn> inputs = new ArrayList<>();
        List<PrivateKey> privateKeys = new ArrayList<>();
        List<RTxOut> outputs = new ArrayList<>();

        public Builder addInput(RTxIn input, PrivateKey key) {
            inputs.add(input);
            privateKeys.add(key);
            return this;
        }

        public Builder addOutput(RTxOut output) {
            outputs.add(output);
            return this;
        }

        public RTransaction build() throws IOException, GeneralSecurityException {
            return RTransaction.createAndSign(
                    inputs.toArray(new RTxIn[inputs.size()]),
                    outputs.toArray(new RTxOut[outputs.size()]),
                    privateKeys.toArray(new PrivateKey[privateKeys.size()])
            );
        }
    }
}
