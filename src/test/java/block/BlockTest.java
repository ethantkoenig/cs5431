package block;

<<<<<<< HEAD
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by EvanKing on 3/2/17.
 */
public class BlockTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testSerialize() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }
}
=======
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Crypto;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class BlockTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws Exception{
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block = Block.empty(previousBlockHash);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = randomTransaction();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    private RTransaction randomTransaction() throws GeneralSecurityException, IOException {
        KeyPair senderPair = Crypto.signatureKeyPair();
        KeyPair recipientPair = Crypto.signatureKeyPair();

        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(randomBytes(256));
        return new RTransaction.Builder()
                .addInput(new RTxIn(hash, 0), senderPair.getPrivate())
                .addOutput(new RTxOut(100, recipientPair.getPublic()))
                .build();
    }
}
>>>>>>> origin/master
