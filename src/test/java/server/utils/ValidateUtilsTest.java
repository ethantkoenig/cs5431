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
    public void testContainsNumber() {
        Assert.assertFalse(ValidateUtils.containsNumber(""));
        Assert.assertFalse(ValidateUtils.containsNumber("##"));

        Assert.assertTrue(ValidateUtils.containsNumber("abc123$S"));
        Assert.assertTrue(ValidateUtils.containsNumber("123"));
        Assert.assertTrue(ValidateUtils.containsNumber("A123"));
        Assert.assertTrue(ValidateUtils.containsNumber("abC123"));
        Assert.assertTrue(ValidateUtils.containsNumber("A123bc"));
    }

    @Test
    public void testContainsLowercase() {
        Assert.assertFalse(ValidateUtils.containsLowercase(""));
        Assert.assertFalse(ValidateUtils.containsLowercase("##"));
        Assert.assertFalse(ValidateUtils.containsLowercase("123"));
        Assert.assertFalse(ValidateUtils.containsLowercase("A123"));

        Assert.assertTrue(ValidateUtils.containsLowercase("abc123$S"));
        Assert.assertTrue(ValidateUtils.containsLowercase("abC123"));
        Assert.assertTrue(ValidateUtils.containsLowercase("A123bc"));
    }

    @Test
    public void testContainsUppercase() {
        Assert.assertFalse(ValidateUtils.containsUpperCase(""));
        Assert.assertFalse(ValidateUtils.containsUpperCase("##"));
        Assert.assertFalse(ValidateUtils.containsUpperCase("123"));
        Assert.assertFalse(ValidateUtils.containsUpperCase("abc123$"));

        Assert.assertTrue(ValidateUtils.containsUpperCase("abC123"));
        Assert.assertTrue(ValidateUtils.containsUpperCase("A123"));
        Assert.assertTrue(ValidateUtils.containsUpperCase("A123bc"));
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
        Assert.assertFalse(ValidateUtils.validPassword("ThisIsAVeryLongLongPassw0rd"));
        Assert.assertFalse(ValidateUtils.validPassword("thisisalll0wercase"));

        Assert.assertTrue(ValidateUtils.validPassword("ThisIsAGoodPassw0rd"));
    }

    @Test
    public void testValidEmail() {
        Assert.assertFalse(ValidateUtils.validEmail(""));
        Assert.assertFalse(ValidateUtils.validEmail("blah"));
        Assert.assertFalse(ValidateUtils.validEmail("email@email"));

        Assert.assertTrue(ValidateUtils.validEmail("test@test.com"));

    }
}
