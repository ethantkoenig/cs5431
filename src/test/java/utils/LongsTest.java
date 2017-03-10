package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

public class LongsTest extends RandomizedTest {

    @Test
    public void testSumWillOverflow() {
        long a, b;
        a = random.nextInt();
        b = random.nextInt();
        Assert.assertFalse(errorMessage, Longs.sumWillOverflow(a, b));

        a = nonNegativeLong();
        b = Long.MAX_VALUE - a + 1;
        Assert.assertTrue(errorMessage, Longs.sumWillOverflow(a, b));

        a = -nonNegativeLong();
        b = Long.MIN_VALUE - (a + 1);
        Assert.assertTrue(errorMessage, Longs.sumWillOverflow(a, b));
    }
}
