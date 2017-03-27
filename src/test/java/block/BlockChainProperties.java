package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import generators.model.SigningKeyPairGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;
import utils.Crypto;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.File;
import java.security.*;
import java.util.ArrayList;
import java.util.Map;

@RunWith(JUnitQuickcheck.class)
public class BlockChainProperties {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Before
    public void setConfig() {
        Config.HASH_GOAL.set(0);

    }

    @Property(trials = 10)
    public void deserializeSerializeInverse(BlockChain blockchain) throws Exception {
        // Store blockchain, reload
        blockchain.storeBlockChain();
        BlockChain bc = new BlockChain();
        bc.importBlockChain(new File("blockchain" + blockchain.getCurrentHead().getShaTwoFiftySix()));

        // Get UTXO set, check equality
        UnspentTransactions oldutxos = blockchain.getUnspentTransactionsAt(blockchain.getCurrentHead());
        UnspentTransactions newutxos = bc.getUnspentTransactionsAt(bc.getCurrentHead());
        Assert.assertTrue(oldutxos.equals(newutxos));

        Map<PublicKey,PrivateKey> keys = SigningKeyPairGenerator.getKeyMapping();
        ArrayList<Transaction> txs = new ArrayList<>();

        for (Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut> utxo : newutxos) {
            Transaction tx = new Transaction.Builder()
                    .addInput(new TxIn(utxo.getKey().getLeft(), utxo.getKey().getRight()), keys.get(utxo.getValue().ownerPubKey))
                    .addOutput(new TxOut(utxo.getValue().value, utxo.getValue().ownerPubKey))
                    .build();
            txs.add(tx);
        }

        Block newblock = Block.empty(bc.getCurrentHead().getShaTwoFiftySix());
        for (Transaction tx : txs) {
            newblock.addTransaction(tx);
            if (newblock.isFull()) {
                break;
            }
        }

        PublicKey key = Crypto.signatureKeyPair().getPublic();
        newblock.addReward(key);

        // Verify the block against the old UTXO set and the new one, then insert
        newblock.verify(newutxos);
        newblock.verify(oldutxos);
        bc.insertBlock(newblock);
        bc.destroyBlockchain(new File("blockchain" + blockchain.getCurrentHead().getShaTwoFiftySix()));
    }
}