package server.models;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class KeyTest {

    @Property
    public void gettersAgreeWithConstructor(int id, int userId, byte[] publicKey, String encryptedPrivateKey) throws Exception {
        Key key = new Key(id, userId, publicKey, encryptedPrivateKey);
        Assert.assertEquals(id, key.getId());
        Assert.assertEquals(userId, key.getUserId());
        Assert.assertArrayEquals(publicKey, key.getPublicKey());
        Assert.assertEquals(encryptedPrivateKey, key.encryptedPrivateKey);
    }

}
