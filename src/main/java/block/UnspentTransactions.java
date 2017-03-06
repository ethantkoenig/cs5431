package block;

import transaction.RTxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.HashMap;
import java.util.Map;

/**
 * A map from (SHA-256, index) pairs to unspent transaction outputs.
 */
public class UnspentTransactions {
    private final Map<Pair<ShaTwoFiftySix, Integer>, RTxOut> map;

    private UnspentTransactions(Map<Pair<ShaTwoFiftySix, Integer>, RTxOut> map) {
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

    public RTxOut put(ShaTwoFiftySix hash, int index, RTxOut out) {
        return map.put(new Pair<>(hash, index), out);
    }

    public RTxOut get(ShaTwoFiftySix hash, int index) {
        return map.get(new Pair<>(hash, index));
    }

    public RTxOut remove(ShaTwoFiftySix hash, int index) {
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
