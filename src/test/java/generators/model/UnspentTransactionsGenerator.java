package generators.model;

import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import testutils.Generators;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnspentTransactionsGenerator extends Generator<UnspentTransactions> {

    private Size size;

    private static final int DEFAULT_MIN_SIZE = 0;
    private static final int DEFAULT_MAX_SIZE = 20;

    public UnspentTransactionsGenerator() {
        super(UnspentTransactions.class);
    }

    public void configure(Size size) {
        this.size = size;
    }

    @Override
    public UnspentTransactions generate(SourceOfRandomness random, GenerationStatus status) {
        return generateWithKeys(random, status).getLeft();
    }

    public Pair<UnspentTransactions, Map<PublicKey, PrivateKey>> generateWithKeys(
            SourceOfRandomness random,
            GenerationStatus status) {
        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        int minSize = size == null ? DEFAULT_MIN_SIZE : size.min();
        int maxSize = size == null ? DEFAULT_MAX_SIZE : size.max();
        int numTxs = random.nextInt(minSize, maxSize);

        ArrayList<ShaTwoFiftySix> hashes = Generators.generateList(
                numTxs, gen().type(ShaTwoFiftySix.class), random, status);

        ArrayList<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < numTxs; ++i) {
            offsets.add(random.nextInt(0,15));
        }

        TxOutGenerator outGen = new TxOutGenerator();
        ArrayList<TxOut> outputs = new ArrayList<>();
        HashMap<PublicKey, PrivateKey> keyMapping = new HashMap<>();
        for (int i = 0; i < numTxs; ++i) {
            Pair<TxOut,KeyPair> p = outGen.generateWithKeys(random, status);
            outputs.add(p.getLeft());
            keyMapping.put(p.getRight().getPublic(), p.getRight().getPrivate());
        }

        for (int i = 0; i < numTxs; ++i) {
            unspentTxs.put(hashes.get(0), offsets.get(0), outputs.get(0));
        }

        return new Pair<>(unspentTxs, keyMapping);
    }
}
