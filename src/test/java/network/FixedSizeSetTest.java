package network;

import org.junit.Assert;
import org.junit.Test;

public class FixedSizeSetTest {

    @Test
    public void testAdd() throws Exception {
        FixedSizeSet<Integer> set = new FixedSizeSet<>();
        for (int i = 1; i <= 101; i++) {
            set.add(i);
        }
        Assert.assertFalse(set.contains(1));
        Assert.assertTrue(set.contains(2));
    }

    @Test
    public void testContains() throws Exception {

    }
}