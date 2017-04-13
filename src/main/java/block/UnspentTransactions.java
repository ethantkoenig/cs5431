package block;

import crypto.ECDSAPublicKey;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A map from (SHA-256, index) pairs to unspent transaction outputs.
 */
public class UnspentTransactions
        implements Iterable<Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut>> {
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

    public int size() { return map.size(); }

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

    @Override
    public Iterator<Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut>> iterator() {
        return map.entrySet().iterator();
    }

    /**
     *
     * @param keys is a list of public keys tied to a user who wants to know how many coins
     *             they own.
     * @return the number of coins tied to these public keys.
     */
    public long getAmounts(ECDSAPublicKey[] keys) {
        return map.values().parallelStream()
                .filter(out -> Arrays.stream(keys)
                        .anyMatch(out.ownerPubKey::equals))
                .mapToLong(out -> out.value)
                .sum();
    }

    /**
     *
     * @param keys is a list of public keys associated with a user
     * @return a list of hashes and indexes that can be used to query the map for the UTXO's associated
     *         with the given keys.
     */
    private List<Pair<ShaTwoFiftySix, Integer>> getHashes(ECDSAPublicKey[] keys) {
        return map.entrySet().parallelStream()
                .filter(entry -> Arrays.stream(keys)
                        .anyMatch(entry.getValue().ownerPubKey::equals))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    /**
     *
     * @param publicKeys is an array of public keys associated with the user wishing
     *                   to construct a transaction.
     * @param masterKey is the key change is to be sent to.
     * @param destination is the receiver public key.
     * @param amount is the amount to be sent.
     *
     * @return a transaction from the user's public keys to the destination
     *         public key. `Optional.empty()` if invalid amount given.
     *
     */
    public Optional<Transaction>
    buildUnsignedTransaction(ECDSAPublicKey[] publicKeys, ECDSAPublicKey masterKey,
                             ECDSAPublicKey destination, long amount) throws IOException {
        // TODO: check for overflow
        // It doesn't matter, in the sense that it's unlikely, and our nodes
        // would reject it anyway, but we shouldn't construct faulty transactions.
        //
        // Proper handling would really have us split an overflowing transaction
        // into multiple transactions, so the return type of this method would
        // have to change.

        // Get hashes, indices of UTXO's - add as many as needed to reach amount.
        List<Pair<ShaTwoFiftySix, Integer>> hashes = getHashes(publicKeys);
        long toBeSpent = 0;
        List<TxIn> txIns = new ArrayList<>();
        for (Pair<ShaTwoFiftySix, Integer> txId : hashes) {
            txIns.add(new TxIn(txId.getLeft(), txId.getRight()));
            toBeSpent += map.get(txId).value;
            if (toBeSpent >= amount) break;
        }

        // Return `Optional.empty()` if insufficient funds
        if (toBeSpent < amount) return Optional.empty();

        Transaction.Builder txb = new Transaction.Builder();
        for (TxIn in: txIns) {
            txb.addInputUnsigned(in);
        }

        // construct two outputs - one to the destination
        txb.addOutput(new TxOut(amount, destination));
        // - and one to the master key for change
        if (toBeSpent > amount) {
            txb.addOutput(new TxOut(toBeSpent - amount, masterKey));
        }

        return Optional.of(txb.buildUnsigned());
    }
}
