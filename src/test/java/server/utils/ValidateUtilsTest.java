package server.utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

public class ValidateUtilsTest extends RandomizedTest {

    @Test
    public void testIsAlphanumeric() {
        Assert.assertFalse(ValidateUtils.isAlphanumeric("##"));
        Assert.assertFalse(ValidateUtils.isAlphanumeric("abc123$S"));

        Assert.assertTrue(ValidateUtils.isAlphanumeric(""));
        Assert.assertTrue(ValidateUtils.isAlphanumeric("Abc"));
        Assert.assertTrue(ValidateUtils.isAlphanumeric("123"));
        Assert.assertTrue(ValidateUtils.isAlphanumeric("A123"));
        Assert.assertTrue(ValidateUtils.isAlphanumeric("abC123"));
        Assert.assertTrue(ValidateUtils.isAlphanumeric("A123bc"));
    }

    @Test
    public void testValidUsername() {
        Assert.assertFalse(ValidateUtils.validUsername(""));
        Assert.assertFalse(ValidateUtils.validUsername("not@l#panumer;("));
        Assert.assertFalse(ValidateUtils.validUsername("ThisUsernameIsQuiteLongIDoSay"));

        Assert.assertTrue(ValidateUtils.validUsername("goodUsername"));
        Assert.assertTrue(ValidateUtils.validUsername("goodUs3rname"));
        Assert.assertTrue(ValidateUtils.validUsername("12BigCats"));
    }

    @Test
    public void testValidPassword() {
        Assert.assertFalse(ValidateUtils.validPassword(""));
        Assert.assertFalse(ValidateUtils.validPassword("password"));
        Assert.assertFalse(ValidateUtils.validPassword("thisisalll0wercase"));
        Assert.assertFalse(ValidateUtils.validPassword("ThisIsAGoodPassw0rd"));
        Assert.assertFalse(ValidateUtils.validPassword("ThisIsAVeryLongLongPassw0rd"));

        Assert.assertTrue(ValidateUtils.validPassword(randomShaTwoFiftySix().toString()));
    }

    @Test
    public void testValidEmail() {
        Assert.assertFalse(ValidateUtils.validEmail(""));
        Assert.assertFalse(ValidateUtils.validEmail("blah"));
        Assert.assertFalse(ValidateUtils.validEmail("email@email"));

        Assert.assertTrue(ValidateUtils.validEmail("test@test.com"));

    }
}
