package block;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import crypto.ECDSAPublicKey;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(JUnitQuickcheck.class)
public class UnspentTransactionsProperties {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Property
    public void builtTransactionHasCorrectAmountAndChange(
            @Size(min=10, max=20) UnspentTransactions utx, @InRange(min="0",max="1") float proportionToSpend) throws Exception {
        List<ECDSAPublicKey> keys = new ArrayList<>();
        for (Map.Entry<Pair<ShaTwoFiftySix,Integer>, TxOut> entry: utx) {
            keys.add(entry.getValue().ownerPubKey);
        }
        ECDSAPublicKey[] keyArr = keys.toArray(new ECDSAPublicKey[keys.size()]);
        long available = utx.getAmounts(keyArr);
        long toSpend = (long) (proportionToSpend * available);

        if (toSpend == 0) return;

        Transaction result = utx.buildUnsignedTransaction(keyArr, keyArr[0], keyArr[1], toSpend).get();

        TxOut toSpender = result.getOutput(0);
        Assert.assertEquals(toSpender.ownerPubKey, keyArr[1]);
        Assert.assertEquals(toSpender.value, toSpend);

        long amountIn = 0;
        for (int i = 0; i < result.numInputs; ++i) {
            TxIn input = result.getInput(i);
            amountIn += utx.get(input.previousTxn, input.txIdx).value;
        }

        if (toSpend != amountIn) {
            TxOut change = result.getOutput(1);
            Assert.assertEquals(change.ownerPubKey, keyArr[0]);
            Assert.assertEquals(change.value, amountIn - toSpend);
        }
    }
}
