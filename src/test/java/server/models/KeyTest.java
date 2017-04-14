package server.models;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class KeyTest {

    @Property
    public void gettersAgreeWithConstructor(byte[] publicKey, String encryptedPrivateKey) throws Exception {
        Key key = new Key(publicKey, encryptedPrivateKey);
        Assert.assertArrayEquals(publicKey, key.getPublicKey());
        Assert.assertEquals(encryptedPrivateKey, key.encryptedPrivateKey);
    }

}
