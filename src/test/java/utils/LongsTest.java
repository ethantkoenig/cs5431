package utils;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.runner.RunWith;
import testutils.RandomizedTest;

@RunWith(JUnitQuickcheck.class)
public class LongsTest extends RandomizedTest {

    @Property
    public void testSumWillOverflow(long a, long b) {
        long sum = a + b;
        Assert.assertEquals(
                Long.signum(a) == Long.signum(b) && Long.signum(sum) != Long.signum(b),
                Longs.sumWillOverflow(a, b));
    }
}
