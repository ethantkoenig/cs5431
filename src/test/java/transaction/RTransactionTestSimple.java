package transaction;

import static org.junit.Assert.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.SeededRandom;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import utils.Crypto;

/**
 * Constructs a basic transaction between 2 peers, and signs and verifies (sig).
 */

public class RTransactionTestSimple {

    @Test
    public void doTransaction() throws Exception {
        Crypto.init();
        KeyPair pairone = Crypto.signatureKeyPair();
        assertNotNull(pairone);
        PublicKey myPublicKey = pairone.getPublic();
        PrivateKey myPrivateKey = pairone.getPrivate();
        KeyPair pairtwo = Crypto.signatureKeyPair();
        assertNotNull(pairtwo);
        PublicKey newPublicKey = pairtwo.getPublic();

        RTransaction tx = new RTransaction();
        byte[][] hash = new byte[1][32];
        hash[0] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[0] = Crypto.sha256(hash[0]);
        int[] idx = new int[1];
        idx[0] = 0;
        PublicKey[] newpubkey = new PublicKey[1];
        newpubkey[0] = newPublicKey;
        boolean add_txin = tx.addTxIns(1, hash, idx);
        Assert.assertTrue(add_txin);

        long[] amt = new long[1];
        amt[0] = 5;
        boolean add_txout = tx.addTxOuts(1, amt, newpubkey);
        Assert.assertTrue(add_txout);

        tx.signInputs(myPrivateKey);
        PublicKey[] pub = new PublicKey[1];
        pub[0] = myPublicKey;
        boolean result = tx.verifySig(pub);
        Assert.assertTrue(result);
    }

//  Sign transaction with key that does not match, should fail.
    @Test
    public void failTransaction() throws Exception {
        Crypto.init();
        KeyPair pairone = Crypto.signatureKeyPair();
        assertNotNull(pairone);
        PublicKey myPublicKey = pairone.getPublic();
        KeyPair pairtwo = Crypto.signatureKeyPair();
        assertNotNull(pairtwo);
        PublicKey newPublicKey = pairtwo.getPublic();
        PrivateKey newPrivateKey = pairtwo.getPrivate();

        RTransaction tx = new RTransaction();
        byte[][] hash = new byte[1][32];
        hash[0] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[0] = Crypto.sha256(hash[0]);
        int[] idx = new int[1];
        idx[0] = 0;
        PublicKey[] newpubkey = new PublicKey[1];
        newpubkey[0] = newPublicKey;
        boolean add_txin = tx.addTxIns(1, hash, idx);
        Assert.assertTrue(add_txin);

        long[] amt = new long[1];
        amt[0] = 5;
        boolean add_txout = tx.addTxOuts(1, amt, newpubkey);
        Assert.assertTrue(add_txout);

        tx.signInputs(newPrivateKey);
        PublicKey[] pub = new PublicKey[1];
        pub[0] = myPublicKey;
        boolean result = tx.verifySig(pub);
        Assert.assertFalse(result);
    }

    @Test
    public void doThreeOutTransaction() throws Exception {
        Crypto.init();
        KeyPair pairone = Crypto.signatureKeyPair();
        assertNotNull(pairone);
        PublicKey myPublicKey = pairone.getPublic();
        PrivateKey myPrivateKey = pairone.getPrivate();

//      Output Address 1
        KeyPair pairtwo = Crypto.signatureKeyPair();
        assertNotNull(pairtwo);
        PublicKey onePublicKey = pairtwo.getPublic();

//      Output Address 2
        KeyPair pairthree = Crypto.signatureKeyPair();
        assertNotNull(pairthree);
        PublicKey twoPublicKey = pairthree.getPublic();

//      Output Address 3
        KeyPair pairfour = Crypto.signatureKeyPair();
        assertNotNull(pairfour);
        PublicKey threePublicKey = pairfour.getPublic();

        RTransaction tx = new RTransaction();
        byte[][] hash = new byte[1][32];
        hash[0] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[0] = Crypto.sha256(hash[0]);
        int[] idx = new int[1];
        idx[0] = 0;
        PublicKey[] newpubkey = new PublicKey[3];
        newpubkey[0] = onePublicKey;
        newpubkey[1] = twoPublicKey;
        newpubkey[2] = threePublicKey;
        boolean add_txin = tx.addTxIns(1, hash, idx);
        Assert.assertTrue(add_txin);

        long[] amt = new long[3];
        amt[0] = 1;
        amt[1] = 2;
        amt[2] = 3;
        boolean add_txout = tx.addTxOuts(3, amt, newpubkey);
        Assert.assertTrue(add_txout);

        tx.signInputs(myPrivateKey);
        PublicKey[] pub = new PublicKey[3];
        pub[0] = myPublicKey;
        pub[1] = myPublicKey;
        pub[2] = myPublicKey;
        boolean result = tx.verifySig(pub);
        Assert.assertTrue(result);
    }

    @Test
    public void doMultiInOutTransaction() throws Exception {
        Crypto.init();
        KeyPair pairone = Crypto.signatureKeyPair();
        assertNotNull(pairone);
        PublicKey myPublicKey = pairone.getPublic();
        PrivateKey myPrivateKey = pairone.getPrivate();

//      Output Address 1
        KeyPair pairtwo = Crypto.signatureKeyPair();
        assertNotNull(pairtwo);
        PublicKey onePublicKey = pairtwo.getPublic();

//      Output Address 2
        KeyPair pairthree = Crypto.signatureKeyPair();
        assertNotNull(pairthree);
        PublicKey twoPublicKey = pairthree.getPublic();

//      Output Address 3
        KeyPair pairfour = Crypto.signatureKeyPair();
        assertNotNull(pairfour);
        PublicKey threePublicKey = pairfour.getPublic();

        RTransaction tx = new RTransaction();
        byte[][] hash = new byte[3][32];
        hash[0] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[0] = Crypto.sha256(hash[0]);
        hash[1] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[1] = Crypto.sha256(hash[0]);
        hash[2] = Crypto.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[2] = Crypto.sha256(hash[0]);

        int[] idx = new int[3];
        idx[0] = 0;
        idx[1] = 1;
        idx[2] = 2;
        PublicKey[] newpubkey = new PublicKey[3];
        newpubkey[0] = onePublicKey;
        newpubkey[1] = twoPublicKey;
        newpubkey[2] = threePublicKey;
        boolean add_txin = tx.addTxIns(3, hash, idx);
        Assert.assertTrue(add_txin);

        long[] amt = new long[3];
        amt[0] = 1;
        amt[1] = 2;
        amt[2] = 3;
        boolean add_txout = tx.addTxOuts(3, amt, newpubkey);
        Assert.assertTrue(add_txout);

        tx.signInputs(myPrivateKey);
        PublicKey[] pub = new PublicKey[3];
        pub[0] = myPublicKey;
        pub[1] = myPublicKey;
        pub[2] = myPublicKey;
        boolean result = tx.verifySig(pub);
        Assert.assertTrue(result);
    }




}