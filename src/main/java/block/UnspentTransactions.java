package block;

import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;
import transaction.Transaction;
import transaction.TxIn;

import java.io.IOException;
import java.util.*;


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
     * @param keys is a list of publics key tied to a user who wants to know how many coins
     *             they own.
     * @return the number of coins tied to these public keys.
     */
    public long getAmounts(ECDSAPublicKey[] keys) {
        long amt = 0;
        for (Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut> entry : map.entrySet())
            for (ECDSAPublicKey key : keys) {
                if (key.equals(entry.getValue().getKey())) {
                    amt += entry.getValue().getValue();
                }
            }
        return amt;
    }

    /**
     *
     * @param keys is a list of public keys associated with a user
     * @return a list of hashes and indexes that can be used to query the map for the UTXO's associated
     *         with the given keys.
     */
    private ArrayList<Pair<ShaTwoFiftySix, Integer>> getHashes(ECDSAPublicKey[] keys) {
        ArrayList<Pair<ShaTwoFiftySix, Integer>> hashes = new ArrayList<>();
        for (Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut> entry : map.entrySet())
            for (ECDSAPublicKey key : keys) {
                if (key.equals(entry.getValue().getKey())) {
                    hashes.add(entry.getKey());
                }
            }
        return hashes;
    }

    /**
     *
     * @param keypairs is a list of keypairs associated with the user wishing to construct
     *                 a transaction.
     * @param masterkey is the key change is to be sent to.
     * @param destination is the receiver public key.
     * @param amount is the amount to be sent.
     * @return a transaction from the user's public keys to the destination public key. Null if invalid amount given.
     *
     */
    public Transaction buildUnsignedTransaction(ECDSAKeyPair[] keypairs, ECDSAPublicKey masterkey,
                                        ECDSAPublicKey destination, long amount) throws IOException {
        // Generate public keys
        ECDSAPublicKey[] publickeys = new ECDSAPublicKey[keypairs.length];
        for (int i = 0; i < keypairs.length; i++)
            publickeys[i] = keypairs[i].publicKey;

        // Get hashes, indexes of UTXO's - add as many as needed to reach amount.
        ArrayList<Pair<ShaTwoFiftySix, Integer>> hashes = getHashes(publickeys);
        long toBeSpent = 0;
        ArrayList<TxIn> txins = new ArrayList<>();
        for (Pair<ShaTwoFiftySix, Integer> txid : hashes) {
            txins.add(new TxIn(txid.getLeft(), txid.getRight()));
            toBeSpent += map.get(txid).getValue();
            if (toBeSpent > amount) break;
        }

        // Return null if insufficient funds, otherwise construct 2 outputs - one to the destination,
        // and one to the master key for change.
        if (toBeSpent < amount) return null;

        TxOut[] outputs = new TxOut[2];
        outputs[0] = new TxOut(amount, destination);
        outputs[1] = new TxOut(toBeSpent - amount, masterkey);

        // TODO: Construct Transaction. Need to build an unsigned transaction
        // Make findbugs pass. This doesn't do what we want it to, it directly performs the signing.
        Transaction.Builder txb = new Transaction.Builder();
        for (int j = 0; j < txins.size(); j++) {
            // Don't use this it will obviously NPE
            txb.addInput(txins.get(j), keypairs[j].privateKey);
        }
        txb.addOutput(outputs[0]);
        txb.addOutput(outputs[1]);

        return txb.build();
    }
}
