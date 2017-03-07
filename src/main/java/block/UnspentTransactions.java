package block;

import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.HashMap;
import java.util.Map;

/**
 * A map from (SHA-256, index) pairs to unspent transaction outputs.
 */
public class UnspentTransactions {
    private final Map<Pair<ShaTwoFiftySix, Integer>, TxOut> map;

    private UnspentTransactions(Map<Pair<ShaTwoFiftySix, Integer>, TxOut> map) {
        this.map = map;
    }

    /**
     * @return A newly-created empty unspent transactions map
     */
    public static UnspentTransactions empty() {
        return new UnspentTransactions(new HashMap<>());
    }

    /**
     * @return A copy of this map
     */
    public UnspentTransactions copy() {
        return new UnspentTransactions(new HashMap<>(map));
    }

    public boolean contains(ShaTwoFiftySix hash, int index) {
        return map.containsKey(new Pair<>(hash, index));
    }

    public TxOut put(ShaTwoFiftySix hash, int index, TxOut out) {
        return map.put(new Pair<>(hash, index), out);
    }

    public TxOut get(ShaTwoFiftySix hash, int index) {
        return map.get(new Pair<>(hash, index));
    }

    public TxOut remove(ShaTwoFiftySix hash, int index) {
        return map.remove(new Pair<>(hash, index));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof UnspentTransactions)) {
            return false;
        }
        UnspentTransactions other = (UnspentTransactions) o;
        return map.equals(other.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
