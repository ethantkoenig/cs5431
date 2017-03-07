package block;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.TxOut;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class UnspentTransactionsTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testPutContains() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(1024));

        KeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.getPublic());
        ut.put(hash, 0, out);

        Assert.assertTrue(errorMessage, ut.contains(hash, 0));
        Assert.assertFalse(errorMessage, ut.contains(hash, 1));

        ShaTwoFiftySix anotherHash = ShaTwoFiftySix.hashOf(randomBytes(1024));
        Assert.assertFalse(errorMessage, ut.contains(anotherHash, 0));
    }

    @Test
    public void testPutGetRemove() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(1024));

        KeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.getPublic());
        ut.put(hash, 0, out);

        TxOut got = ut.get(hash, 0);
        Assert.assertEquals(errorMessage, got, out);

        TxOut removed = ut.remove(hash, 0);
        Assert.assertEquals(errorMessage, removed, out);
        Assert.assertFalse(errorMessage, ut.contains(hash, 0));

        Assert.assertNull(errorMessage, ut.get(hash, 0));
        Assert.assertNull(errorMessage, ut.remove(hash, 0));
    }

    @Test
    public void testCopy() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(1024));

        KeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.getPublic());
        ut.put(hash, 0, out);

        UnspentTransactions copy = ut.copy();

        Assert.assertEquals(errorMessage, copy, ut);
        Assert.assertEquals(errorMessage, copy.hashCode(), ut.hashCode());
        Assert.assertTrue(copy.contains(hash, 0));
        Assert.assertFalse(copy.contains(hash, 1));
        TxOut got = copy.get(hash, 0);
        Assert.assertEquals(errorMessage, got, out);
    }
}
