package block;

import crypto.ECDSAPublicKey;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Longs;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A map from (SHA-256, index) pairs to unspent transaction outputs.
 */
public class UnspentTransactions implements Iterable<Map.Entry<TxIn, TxOut>> {
    private final Map<TxIn, TxOut> map;

    private UnspentTransactions(Map<TxIn, TxOut> map) {
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
        return map.containsKey(new TxIn(hash, index));
    }

    public TxOut put(ShaTwoFiftySix hash, int index, TxOut out) {
        return map.put(new TxIn(hash, index), out);
    }

    public TxOut get(ShaTwoFiftySix hash, int index) {
        return map.get(new TxIn(hash, index));
    }

    public TxOut remove(ShaTwoFiftySix hash, int index) {
        return map.remove(new TxIn(hash, index));
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
    public Iterator<Map.Entry<TxIn, TxOut>> iterator() {
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
     * @return a list of `UnspentOutputs` owned by these `keys` that can be used to build a `Transaction`
     */
    private List<UnspentOutput> getUnspentOutputs(List<ECDSAPublicKey> keys) {
        return map.entrySet().stream()
                .filter(entry -> keys.stream()
                        .anyMatch(entry.getValue().ownerPubKey::equals))
                .map(entry -> new UnspentOutput(
                        entry.getValue().ownerPubKey,
                        entry.getKey().previousTxn,
                        entry.getKey().txIdx,
                        entry.getValue().value))
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
    public Optional<Pair<List<ECDSAPublicKey>,Transaction>>
    buildUnsignedTransaction(List<ECDSAPublicKey> publicKeys, ECDSAPublicKey masterKey,
                             ECDSAPublicKey destination, long amount) throws IOException {

        if (amount <= 0) {
            return Optional.empty();
        }

        // Get hashes, indices of UTXO's - add as many as needed to reach amount.
        List<UnspentOutput> hashes = getUnspentOutputs(publicKeys);
        long toBeSpent = 0;
        List<TxIn> txIns = new ArrayList<>();
        List<ECDSAPublicKey> keysUsed = new ArrayList<>();
        for (UnspentOutput utx: hashes) {
            txIns.add(new TxIn(utx.txHash, utx.index));
            if (Longs.sumWillOverflow(toBeSpent, utx.value)) return Optional.empty();
            toBeSpent += utx.value;
            keysUsed.add(utx.ownerKey);
            if (toBeSpent >= amount) break;
        }

        // Return `Optional.empty()` if insufficient funds
        if (toBeSpent < amount) return Optional.empty();

        Transaction.UnsignedBuilder txb = new Transaction.UnsignedBuilder();
        for (TxIn input: txIns) {
            txb.addInput(input);
        }

        // construct two outputs - one to the destination
        txb.addOutput(new TxOut(amount, destination));
        // - and one to the master key for change
        if (toBeSpent > amount) {
            txb.addOutput(new TxOut(toBeSpent - amount, masterKey));
        }

        return Optional.of(new Pair<>(keysUsed,txb.build()));
    }

    private static class UnspentOutput {
        private final ECDSAPublicKey ownerKey;
        private final ShaTwoFiftySix txHash;
        private final int index;
        private final long value;

        public UnspentOutput(ECDSAPublicKey ownerKey, ShaTwoFiftySix txHash, int index, long value) {
            this.ownerKey = ownerKey;
            this.txHash = txHash;
            this.index = index;
            this.value = value;
        }
    }
}
