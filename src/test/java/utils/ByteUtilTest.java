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
    public void testBytesToHexString() throws Exception {
        byte[] b = ByteUtil.hexStringToByteArray("e04fd020ea3a6910a2d808002b30309d");
        String byteString = ByteUtil.bytesToHexString(b);
        Assert.assertTrue(errorMessage, "e04fd020ea3a6910a2d808002b30309e".equals(ByteUtil.addOne(byteString)));
    }

    @Test
    public void testConcatenate() throws Exception {
        byte[] a = new byte[]{0,1};
        byte[] b = new byte[]{1,0};
        Assert.assertTrue(errorMessage, Arrays.equals(new byte[]{0,1,1,0}, ByteUtil.concatenate(a, b)));
    }

    @Test
    public void testAddOneString() throws Exception {
        String b = "ae";
        Assert.assertTrue(errorMessage, "af".equals(ByteUtil.addOne(b)));
    }

    @Test
    public void testAddOneByte() throws Exception {
        byte[] a = new byte[]{(byte) 0xa,(byte) 0xe};
        Assert.assertTrue(errorMessage, Arrays.equals(new byte[]{(byte) 0xa,(byte) 0xf}, ByteUtil.addOne(a)));
    }

    @Test
    public void testCompare() throws Exception {
        byte[] a = ByteUtil.hexStringToByteArray("0a2d808002b3030dd");
        byte[] b = ByteUtil.hexStringToByteArray("0a2d808002b30309d");
        Assert.assertTrue(errorMessage, ByteUtil.compare(a,b) == 1);
    }

}