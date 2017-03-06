package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

public class PairTest extends RandomizedTest {

    @Test
    public void testGetLeft() {
        String left = randomAsciiString(random.nextInt(16));
        String right = randomAsciiString(random.nextInt(16));
        Pair<String, String> pair = new Pair<>(left, right);
        Assert.assertEquals(errorMessage, left, pair.getLeft());
    }

    @Test
    public void testGetRight() {
        String left = randomAsciiString(random.nextInt(16));
        String right = randomAsciiString(random.nextInt(16));
        Pair<String, String> pair = new Pair<>(left, right);
        Assert.assertEquals(errorMessage, right, pair.getRight()) ;
    }

    @Test
    public void testEquals() {
        Assert.assertEquals(
                new Pair<>("hello", "world"),
                new Pair<>("hello", "world")
        );
        Assert.assertNotEquals(
                new Pair<>("hello", "world"),
                new Pair<>("goodbye", "world")
        );
        Assert.assertNotEquals(
                new Pair<>("hello", "world"),
                new Pair<>("hello", "mars")
        );
        Assert.assertNotEquals(
                new Pair<>("hello", "world"),
                new Pair<>("goodbye", "mars")
        );
        Assert.assertNotEquals(
                new Pair<>("hello", "world"),
                null
        );
    }

    @Test
    public void testHashcode() {
        String left = randomAsciiString(random.nextInt(16));
        String right = randomAsciiString(random.nextInt(16));
        Pair<String, String> pair1 = new Pair<>(left, right);
        Pair<String, String> pair2 = new Pair<>(left, right);
        Assert.assertEquals(errorMessage, pair1.hashCode(), pair2.hashCode());
    }
}
