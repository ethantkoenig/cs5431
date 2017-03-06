package network;

import block.BlockChain;
import block.UnspentTransactions;

import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Created by EvanKing on 3/5/17.
 */
public class MiningBundle {

    private BlockChain blockChain;

    private KeyPair keyPair;
    public final PublicKey privilegedKey;

    private UnspentTransactions unspentTransactions;

    public MiningBundle(
            BlockChain blockChain,
            KeyPair keyPair,
            PublicKey privilegedKey,
            UnspentTransactions unspentTransactions) {
        this.blockChain = blockChain;
        this.keyPair = keyPair;
        this.privilegedKey = privilegedKey;
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
