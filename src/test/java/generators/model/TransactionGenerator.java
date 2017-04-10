package generators.model;

import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class TransactionGenerator extends Generator<Transaction> {


    private static int DEFAULT_MAX_INPUTS = 5;
    private static int DEFAULT_MAX_OUTPUTS = 5;

    private final Optional<UnspentTransactions> unspentTxs;

    public TransactionGenerator() {
        super(Transaction.class);
        unspentTxs = Optional.empty();
    }

    public TransactionGenerator(UnspentTransactions unspentTxs) {
        super(Transaction.class);
        this.unspentTxs = Optional.of(unspentTxs);
    }

    @Override
    public Transaction generate(SourceOfRandomness random, GenerationStatus status) {
        return unspentTxs
                .map(p -> generateWithRespectTo(p, random, status))
                .orElseGet(() -> generateSimple(random, status));
    }

    private Transaction generateSimple(SourceOfRandomness random, GenerationStatus status) {
        Transaction.Builder builder = new Transaction.Builder();

        int numInputs = random.nextInt(1, DEFAULT_MAX_INPUTS);
        for (int i = 0; i < numInputs; ++i) {
            TxIn in = gen().type(TxIn.class).generate(random, status);
            ECDSAKeyPair keys = gen().type(ECDSAKeyPair.class).generate(random, status);
            builder.addInput(in, keys.privateKey);
        }

        int numOutputs = random.nextInt(1, DEFAULT_MAX_OUTPUTS);
        for (int i = 0; i < numOutputs; ++i) {
            TxOut out = gen().type(TxOut.class).generate(random, status);
            builder.addOutput(out);
        }

        try {
            return builder.build();
        } catch (IOException e) {
            // We should not ever encounter this
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    private Transaction generateWithRespectTo(
            UnspentTransactions unspentTxs,
            SourceOfRandomness random,
            GenerationStatus status) {
        if (unspentTxs.size() <= 0) {
            assert false;
            return null;
        }

        Map<ECDSAPublicKey, ECDSAPrivateKey> keyMapping = SigningKeyPairGenerator.getKeyMapping();

        int numInputs = random.nextInt(1, Math.min(unspentTxs.size(), DEFAULT_MAX_INPUTS));
        int numOutputs = random.nextInt(1, DEFAULT_MAX_OUTPUTS);

        long valueMoved = 0;

        TxInGenerator txInGen = new TxInGenerator();
        SigningKeyPairGenerator keyGen = new SigningKeyPairGenerator();
        Transaction.Builder builder = new Transaction.Builder();

        for (int i = 0; i < numInputs; ++i) {
            Pair<TxOut, TxIn> p = txInGen.generateWithRespectTo(unspentTxs, random, status);
            valueMoved += p.getLeft().value;
            builder.addInput(p.getRight(), keyMapping.get(p.getLeft().ownerPubKey));
        }

        long valueLeft = valueMoved;

        for (int j = 0; j < numOutputs; ++j) {
            ECDSAKeyPair keys = keyGen.generate(random, status);

            long outVal;
            if (j == numOutputs - 1) {
                outVal = valueLeft;
            } else {
                outVal = valueMoved / numOutputs;
            }
            valueLeft -= outVal;

            TxOut out = new TxOut(outVal, keys.publicKey);
            builder.addOutput(out);
        }

        Transaction result;
        try {
            result = builder.build();
        } catch (IOException e) {
            // We should not reach this case unless something goes seriously wrong
            e.printStackTrace();
            assert false;
            return null;
        }

        ShaTwoFiftySix resultHash = result.getShaTwoFiftySix();

        for (int i = 0; i < result.numOutputs; ++i) {
            TxOut out = result.getOutput(i);
            unspentTxs.put(resultHash, i, out);
        }

        return result;
    }

}
