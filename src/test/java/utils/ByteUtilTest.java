package utils;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.util.Arrays;

/**
 * Created by EvanKing on 2/26/17.
 */
public class ByteUtilTest extends RandomizedTest {

    @Test
    public void testConcatenate() throws Exception {
        byte[] a = new byte[]{0,1};
        byte[] b = new byte[]{1,0};
        Assert.assertTrue(errorMessage, Arrays.equals(new byte[]{0,1,1,0}, ByteUtil.concatenate(a, b)));
    }

    @Test
    public void testAddOne() throws Exception {
        byte[] a = new byte[]{1,0};
        Assert.assertTrue(errorMessage, Arrays.equals(new byte[]{1,1}, ByteUtil.addOne(a)));
    }
}