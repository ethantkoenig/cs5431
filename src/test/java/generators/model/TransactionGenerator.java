package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class TransactionGenerator extends Generator<Transaction> {

    private static double FRAC_CORRECT = .8;
    private static int MAX_INPUTS = 5;
    private static int MAX_OUTPUTS = 5;

    public TransactionGenerator() {
        super(Transaction.class);
    }

    @Override
    public Transaction generate(SourceOfRandomness random, GenerationStatus status) {
        Transaction.Builder builder = new Transaction.Builder();

        // TODO: Update this method to generate consistent transactions

        int numInputs = random.nextInt(1,MAX_INPUTS);
        for (int i = 0; i < numInputs; ++i) {
            TxIn in = gen().type(TxIn.class).generate(random, status);
            KeyPair keys = gen().type(KeyPair.class).generate(random, status);
            builder.addInput(in, keys.getPrivate());
        }

        int numOutputs = random.nextInt(1,MAX_OUTPUTS);
        for (int i = 0; i < numOutputs; ++i) {
            TxOut out = gen().type(TxOut.class).generate(random, status);
            builder.addOutput(out);
        }

        try {
            return builder.build();
        } catch (GeneralSecurityException e) {
            // We should not ever encounter this
            assert false;
        } catch (IOException e) {
            // Nor this
            assert false;
        }

        return null;
    }
}
