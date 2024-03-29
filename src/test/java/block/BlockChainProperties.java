package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import generators.model.SigningKeyPairGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;

import java.util.ArrayList;
import java.util.Map;

@RunWith(JUnitQuickcheck.class)
public class BlockChainProperties {

    @Before
    public void setConfig() {
        Config.setHashGoal(0);
    }

    @Property(trials = 1)
    public void deserializeSerializeInverse(BlockChain blockchain, ECDSAKeyPair keyPair) throws Exception {
        // Store blockchain, reload
        BlockChain bc = new BlockChain(blockchain.blockStorePath);

        // Get UTXO set, check equality
        int blockChainHeadDepth = blockchain.getAncestorsStartingAt(
                blockchain.getCurrentHead().getShaTwoFiftySix()
        ).size();
        int bcHeadDepth = bc.getAncestorsStartingAt(bc.getCurrentHead().getShaTwoFiftySix()).size();
        Assert.assertEquals(blockChainHeadDepth, bcHeadDepth);
        UnspentTransactions oldutxos = blockchain.getUnspentTransactionsAt(blockchain.getCurrentHead());
        UnspentTransactions newutxos = bc.getUnspentTransactionsAt(bc.getCurrentHead());
        if (blockchain.getCurrentHead().equals(bc.getCurrentHead())) {
            Assert.assertEquals(oldutxos, newutxos);
        }

        Map<ECDSAPublicKey, ECDSAPrivateKey> keys = SigningKeyPairGenerator.getKeyMapping();
        ArrayList<Transaction> txs = new ArrayList<>();

        for (Map.Entry<TxIn, TxOut> utxo : newutxos) {
            Transaction tx = new Transaction.Builder()
                    .addInput(utxo.getKey(), keys.get(utxo.getValue().ownerPubKey))
                    .addOutput(new TxOut(utxo.getValue().value, utxo.getValue().ownerPubKey))
                    .build();
            txs.add(tx);
            if (txs.size() == Block.NUM_TRANSACTIONS_PER_BLOCK) {
                break;
            }
        }

        Block newblock = Block.block(bc.getCurrentHead().getShaTwoFiftySix(), txs, keyPair.publicKey);

        // Verify the block against the old UTXO set and the new one, then insert
        newblock.verifyNonGenesis(newutxos);
        newblock.verifyNonGenesis(oldutxos);
        bc.insertBlock(newblock);
    }
}
