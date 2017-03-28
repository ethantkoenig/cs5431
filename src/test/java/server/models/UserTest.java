package server.models;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class UserTest {

    @Property
    public void gettersAgreesWithConstructor(int id, String username, byte[] salt, byte[] hashedPassword) throws Exception {
        User user = new User(id, username, salt, hashedPassword);
        Assert.assertEquals(id, user.getId());
        Assert.assertEquals(username, user.getUsername());
        Assert.assertArrayEquals(salt, user.getSalt());
        Assert.assertArrayEquals(hashedPassword, user.getHashedPassword());
    }
}