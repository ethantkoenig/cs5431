package network;

import block.BlockChain;
import block.UnspentTransactions;

import java.security.KeyPair;

/**
 * Created by EvanKing on 3/5/17.
 */
public class MiningBundle {

    private BlockChain blockChain;

    private KeyPair keyPair;

    private UnspentTransactions unspentTransactions;

    public MiningBundle(BlockChain blockChain, KeyPair keyPair, UnspentTransactions unspentTransactions) {
        this.blockChain = blockChain;
        this.keyPair = keyPair;
        this.unspentTransactions = unspentTransactions;
    }

    public void setBlockChain(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public void setUnspentTransactions(UnspentTransactions unspentTransactions) {
        this.unspentTransactions = unspentTransactions;
    }

    public BlockChain getBlockChain() {
        return blockChain;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public UnspentTransactions getUnspentTransactions() {
        return unspentTransactions;
    }
}
