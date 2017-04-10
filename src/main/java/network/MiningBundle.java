package network;

import block.BlockChain;
import block.UnspentTransactions;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;

/**
 * A wrapper object to pass from the network.Miner object to the miner HandleMessageThread
 *
 * @version 1.0, March 7 2017
 */
public class MiningBundle {

    private final BlockChain blockChain;
    private final ECDSAKeyPair keyPair;
    public final ECDSAPublicKey privilegedKey;

    private UnspentTransactions unspentTransactions;

    public MiningBundle(
            BlockChain blockChain,
            ECDSAKeyPair keyPair,
            ECDSAPublicKey privilegedKey,
            UnspentTransactions unspentTransactions) {
        this.blockChain = blockChain;
        this.keyPair = keyPair;
        this.privilegedKey = privilegedKey;
        this.unspentTransactions = unspentTransactions;
    }

    public void setUnspentTransactions(UnspentTransactions unspentTransactions) {
        this.unspentTransactions = unspentTransactions;
    }

    public BlockChain getBlockChain() {
        return blockChain;
    }

    public ECDSAKeyPair getKeyPair() {
        return keyPair;
    }

    public UnspentTransactions getUnspentTransactions() {
        return unspentTransactions;
    }
}
