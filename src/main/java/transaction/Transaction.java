package transaction;


import block.UnspentTransactions;
import utils.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Main transaction class.
 * Contains an array of inputs, outputs and signatures.
 */
public class Transaction extends HashCache implements CanBeSerialized {
    private final static Logger LOGGER = Logger.getLogger(Logger.class.getName());
    public static final Deserializer<Transaction> DESERIALIZER = new TransactionDeserializer();

    private final TxIn[] txIn;
    private final TxOut[] txOut;
    private final Signature[] signatures;

    public final int numInputs;
    public final int numOutputs;

    private Transaction(TxIn[] txIn, TxOut[] txOut, Signature[] signatures) {
        this.txIn = txIn;
        this.txOut = txOut;
        this.signatures = signatures;
        numInputs = txIn.length;
        numOutputs = txOut.length;
    }

    private static Transaction createAndSign(TxIn[] inputs, TxOut[] outputs, PrivateKey[] keys)
            throws IOException, GeneralSecurityException {
        if (inputs.length != keys.length) {
            throw new IllegalStateException();
        }
        Transaction txn = new Transaction(inputs, outputs, null);
        byte[] body = ByteUtil.asByteArray(txn::serializeWithoutSignatures);

        Signature[] signatures = new Signature[keys.length];
        for (int i = 0; i < keys.length; i++) {
            signatures[i] = Signature.sign(body, keys[i]);
        }

        return new Transaction(inputs, outputs, signatures);
    }

    /**
     * Serialize this transaction (excluding signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serializeWithoutSignatures(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeArray(outputStream, txIn);
        CanBeSerialized.serializeArray(outputStream, txOut);
    }

    /**
     * Serialize this transaction (including signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot fully serializeWithoutSignatures unsigned transaction");
        }
        serializeWithoutSignatures(outputStream);
        for (Signature signature : signatures) {
            signature.serialize(outputStream);
        }
    }

    /**
     * Returns the input at position 'index' in the transaction.
     *
     * @param index is the input number to be returned.
     * @return an TxIn object of this transaction at the requested index.
     */
    public TxIn getInput(int index) {
        if (index < 0 || index >= txIn.length) {
            String msg = String.format("Invalid index %d", index);
            throw new IllegalArgumentException(msg);
        }
        return txIn[index];
    }

    /**
     * Returns the output at position 'index' in the transaction.
     *
     * @param index is the output number to be returned.
     * @return an TxOut object of this transaction at the requested index.
     */
    public TxOut getOutput(int index) {
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
     * @throws IOException
     */
    public boolean verifySignature(int inputIndex, PublicKey key)
            throws IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot verify unsigned transaction");
        } else if (inputIndex < 0 || inputIndex >= signatures.length) {
            String msg = String.format("Invalid index: %d, expected to be in range [0, %d)",
                    inputIndex, txIn.length);
            throw new IllegalArgumentException(msg);
        }

        // TODO: eventually find a way to not re-serializeWithoutSignatures every time
        return signatures[inputIndex].verify(ByteUtil.asByteArray(this::serializeWithoutSignatures), key);
    }

    /**
     * Verifies {@code this Transaction} with respect to {@code unspentOutputs}.
     * <p>
     * This will check that all signatures are valid and that there are no double spends.
     *
     * @param unspentOutputs A {@code Map} containing all of the unspent outputs that {@code this} may refer to. Entries
     *                       that are spent by {@code this Transaction} will be removed. If verification fails, there
     *                       are no guarantees as to the state of this {@code Map}.
     * @return Whether this {@code Transaction} was successfully verified.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public boolean verify(UnspentTransactions unspentOutputs)
            throws IOException {
        long inputSum = 0;
        long outputSum = 0;
        for (int i = 0; i < txIn.length; ++i) {
            TxIn in = txIn[i];
            if (!unspentOutputs.contains(in.previousTxn, in.txIdx)) {
                LOGGER.warning("Invalid input: " + in.previousTxn + "," + in.txIdx);
                return false;
            }
            TxOut out = unspentOutputs.remove(in.previousTxn, in.txIdx);
            if (Longs.sumWillOverflow(inputSum, out.value)) {
                return false;
            }
            inputSum += out.value;
            if (!verifySignature(i, out.ownerPubKey)) {
                LOGGER.warning("Invalid signature: " + out.ownerPubKey + "," + signatures[i]);
                return false;
            }
        }
        for (int j = 0; j < txOut.length; j++) {
            TxOut out = txOut[j];
            if (out.value <= 0) {
                return false;
            } else if (Longs.sumWillOverflow(outputSum, out.value)) {
                return false;
            }
            outputSum += out.value;
            unspentOutputs.put(getShaTwoFiftySix(), j, out);
        }
        return outputSum == inputSum;
    }

    /**
     * "Rollbacks" this transaction, by updating {@code unspentTransactions} to
     * reflect the state before this transaction was applied.
     *
     * @param unspentTransactions map of unspent transactions to update
     * @param lookup              way to lookup previous transactions
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
        for (TxIn input : txIn) {
            if (unspentTransactions.contains(input.previousTxn, input.txIdx)) {
                return false;
            }

            Optional<Transaction> opt = lookup.lookup(input.previousTxn);
            if (!opt.isPresent()) {
                return false;
            }

            TxOut output = opt.get().getOutput(input.txIdx);
            unspentTransactions.put(input.previousTxn, input.txIdx, output);
        }
        return true;
    }

    @Override
    protected ShaTwoFiftySix computeHash() {
        try {
            return ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serializeWithoutSignatures));
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction) o;
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
        return Arrays.deepHashCode(new Object[]{txIn, txOut});
    }

    public static class Builder {
        List<TxIn> inputs = new ArrayList<>();
        List<PrivateKey> privateKeys = new ArrayList<>();
        List<TxOut> outputs = new ArrayList<>();

        public Builder addInput(TxIn input, PrivateKey key) {
            inputs.add(input);
            privateKeys.add(key);
            return this;
        }

        public Builder addOutput(TxOut output) {
            outputs.add(output);
            return this;
        }

        public Transaction build() throws IOException, GeneralSecurityException {
            return Transaction.createAndSign(
                    inputs.toArray(new TxIn[inputs.size()]),
                    outputs.toArray(new TxOut[outputs.size()]),
                    privateKeys.toArray(new PrivateKey[privateKeys.size()])
            );
        }
    }

    private static final class TransactionDeserializer implements Deserializer<Transaction> {
        @Override
        public Transaction deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            TxIn[] inputs = Deserializer.deserializeList(inputStream, TxIn.DESERIALIZER)
                    .toArray(new TxIn[0]);

            TxOut[] outputs = Deserializer.deserializeList(inputStream, TxOut.DESERIALIZER)
                    .toArray(new TxOut[0]);

            Signature[] signatures = new Signature[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                signatures[i] = Signature.deserialize(inputStream);
            }
            return new Transaction(inputs, outputs, signatures);
        }
    }
}
