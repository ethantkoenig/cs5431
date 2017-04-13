package block;

import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.TxOut;
import utils.ShaTwoFiftySix;

import java.security.GeneralSecurityException;

public class UnspentTransactionsTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testPutContains() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(1024));

        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.publicKey);
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

        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.publicKey);
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

        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.publicKey);
        ut.put(hash, 0, out);

        UnspentTransactions copy = ut.copy();

        TestUtils.assertEqualsWithHashCode(errorMessage, copy, ut);
        Assert.assertTrue(copy.contains(hash, 0));
        Assert.assertFalse(copy.contains(hash, 1));
        TxOut got = copy.get(hash, 0);
        Assert.assertEquals(errorMessage, got, out);
    }

    @Test
    public void testEquals() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(1024));

        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        TxOut out = new TxOut(1024, pair.publicKey);
        ut.put(hash, 0, out);

        UnspentTransactions copy = ut.copy();

        TestUtils.assertEqualsWithHashCode(errorMessage, ut, ut);
        TestUtils.assertEqualsWithHashCode(errorMessage, copy, ut);
        Assert.assertNotEquals(errorMessage, copy, UnspentTransactions.empty());
        Assert.assertNotEquals(errorMessage, copy, null);
    }

    @Test
    public void testUnsignedTransaction() throws GeneralSecurityException {
        UnspentTransactions ut = UnspentTransactions.empty();
        ShaTwoFiftySix hash1 = ShaTwoFiftySix.hashOf(randomBytes(1024));
        ShaTwoFiftySix hash2 = ShaTwoFiftySix.hashOf(randomBytes(1024));
        ShaTwoFiftySix hash3 = ShaTwoFiftySix.hashOf(randomBytes(1024));
        ShaTwoFiftySix hash4 = ShaTwoFiftySix.hashOf(randomBytes(1024));

        ECDSAKeyPair pair1 = Crypto.signatureKeyPair();
        ECDSAKeyPair pair2 = Crypto.signatureKeyPair();
        ECDSAKeyPair pair3 = Crypto.signatureKeyPair();
        ECDSAKeyPair pair4 = Crypto.signatureKeyPair();
        TxOut out1 = new TxOut(1024, pair1.publicKey);
        TxOut out2 = new TxOut(1024, pair2.publicKey);
        TxOut out3 = new TxOut(1024, pair3.publicKey);
        TxOut out4 = new TxOut(1024, pair4.publicKey);
        ut.put(hash1, 0, out1);
        ut.put(hash2, 0, out2);
        ut.put(hash3, 0, out3);
        ut.put(hash4, 0, out4);

        ECDSAPublicKey[] keys1 = new ECDSAPublicKey[1];
        keys1[0] = pair1.publicKey;
        Assert.assertEquals(1024, ut.getAmounts(keys1));

        ECDSAPublicKey[] keys2 = new ECDSAPublicKey[2];
        keys2[0] = pair1.publicKey;
        keys2[1] = pair2.publicKey;
        Assert.assertEquals(2048, ut.getAmounts(keys2));

        ECDSAPublicKey[] keys3 = new ECDSAPublicKey[3];
        keys3[0] = pair1.publicKey;
        keys3[1] = pair2.publicKey;
        keys3[2] = pair3.publicKey;
        Assert.assertEquals(3072, ut.getAmounts(keys3));

        ECDSAPublicKey[] keys4 = new ECDSAPublicKey[4];
        keys4[0] = pair1.publicKey;
        keys4[1] = pair2.publicKey;
        keys4[2] = pair3.publicKey;
        keys4[3] = pair4.publicKey;
        Assert.assertEquals(4096, ut.getAmounts(keys4));
    }
}
