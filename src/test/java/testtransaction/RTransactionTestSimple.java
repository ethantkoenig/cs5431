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
 * Created by willronchetti on 2/28/17.
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
        hash[0] = tx.convertHashFromString("0000000000000000013332f3ad5c0b3311392a2d48449d560eed01ba23174d48");
        hash[0] = Crypto.sha256(hash[0]);
        int[] idx = new int[1];
        idx[0] = 0;
        PublicKey[] pubkey = new PublicKey[1];
        pubkey[0] = newPublicKey;
        boolean add_txin = tx.addTxIns(1, hash, idx, pubkey);
        Assert.assertTrue(add_txin);

        long[] amt = new long[1];
        amt[0] = 5;
        boolean add_txout = tx.addTxOuts(1, amt, pubkey);
        Assert.assertTrue(add_txout);

        tx.signInputs(myPrivateKey);
        PublicKey[] pub = new PublicKey[1];
        pub[0] = myPublicKey;
        boolean result = tx.verifySig(pub);
        System.out.println(result);
        Assert.assertTrue(result);
    }



}