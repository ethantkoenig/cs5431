package block;

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
import java.security.PrivateKey;
import java.util.Collections;

public class BlockTest extends RandomizedTest {

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Test
    public void testSerialize() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block = Block.empty(previousBlockHash);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = randomTransaction();
        }
        block.addReward(Crypto.signatureKeyPair().getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testSerializeGenesis() throws Exception {
        Block block = Block.genesis();
        block.addReward(Crypto.signatureKeyPair().getPublic());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        block.serialize(new DataOutputStream(outputStream));
        Block deserialized = Block.deserialize(ByteBuffer.wrap(outputStream.toByteArray()));

        Assert.assertEquals(errorMessage,
                block.getShaTwoFiftySix(),
                deserialized.getShaTwoFiftySix()
        );
    }

    @Test
    public void testVerify() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block = Block.empty(previousBlockHash);


        ShaTwoFiftySix initialTransactionHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        KeyPair initialPair = Crypto.signatureKeyPair();
        RTxOut initialOut = new RTxOut(1 + random.nextInt(1024), initialPair.getPublic());

        populate(block, initialTransactionHash,
                 initialOut, 0,
                 initialPair.getPrivate());
        block.addReward(Crypto.signatureKeyPair().getPublic());

        UnspentTransactions unspent = UnspentTransactions.empty();
        unspent.put(initialTransactionHash, 0, initialOut);

        Assert.assertTrue(block.verify(unspent).isPresent());
    }

    @Test
    public void testGetTransactionDifferences() throws Exception {
        ShaTwoFiftySix previousBlockHash = ShaTwoFiftySix.hashOf(randomBytes(256));
        Block block1 = Block.empty(previousBlockHash);
        Block block2 = Block.empty(previousBlockHash);

        RTransaction txn1 = randomTransaction();
        RTransaction txn2 = randomTransaction();
        RTransaction txn3 = randomTransaction();

        block1.addTransaction(txn1);
        block1.addTransaction(txn2);
        block2.addTransaction(txn2);
        block2.addTransaction(txn3);

        Assert.assertEquals(errorMessage,
                block1.getTransactionDifferences(block2),
                Collections.singletonList(txn3));
        Assert.assertEquals(errorMessage,
                block2.getTransactionDifferences(block1),
                Collections.singletonList(txn1));
    }

    private void populate(
            Block block,
            ShaTwoFiftySix initialHash,
            RTxOut initialInput,
            int initialInputIndex,
            PrivateKey initialInputKey) throws GeneralSecurityException, IOException {
        KeyPair recipientPair = Crypto.signatureKeyPair();
        block.transactions[0] = new RTransaction.Builder()
                .addInput(new RTxIn(initialHash, initialInputIndex), initialInputKey)
                .addOutput(new RTxOut(initialInput.value, recipientPair.getPublic()))
                .build();

        for (int i = 1; i < Block.NUM_TRANSACTIONS_PER_BLOCK; i++) {
            KeyPair senderPair = recipientPair;
            recipientPair = Crypto.signatureKeyPair();
            block.transactions[i] = new RTransaction.Builder()
                    .addInput(
                            new RTxIn(
                                    block.transactions[i-1].getShaTwoFiftySix(),
                                    0
                            ),
                            senderPair.getPrivate()
                    )
                    .addOutput(new RTxOut(initialInput.value, recipientPair.getPublic()))
                    .build();
        }
    }
 }
