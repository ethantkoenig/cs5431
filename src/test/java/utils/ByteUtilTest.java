package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.util.Arrays;

import static testutils.TestUtils.assertPresent;
import static testutils.TestUtils.bytes;

public class ByteUtilTest extends RandomizedTest {

    @Test
    public void testConcatenate() throws Exception {
        byte[] a = new byte[]{0, 1};
        byte[] b = new byte[]{1, 0};
        Assert.assertTrue(errorMessage, Arrays.equals(new byte[]{0, 1, 1, 0}, ByteUtil.concatenate(a, b)));
    }

    @Test
    public void testAddOneByte() throws Exception {
        byte[] a = bytes(0x0a, 0xfe);
        ByteUtil.addOne(a);
        Assert.assertArrayEquals(errorMessage, bytes(0x0a, 0xff), a);

        a = bytes(0x0a, 0xff);
        ByteUtil.addOne(a);
        Assert.assertArrayEquals(errorMessage, bytes(0x0b, 0x00), a);

        a = bytes(0xff, 0xff);
        ByteUtil.addOne(a);
        Assert.assertArrayEquals(bytes(0, 0), a);
    }

    @Test
    public void testCompare() throws Exception {
        byte[] a = assertPresent(
                ByteUtil.hexStringToByteArray("0a2d808002b3030dd")
        );
        byte[] b = assertPresent(
                ByteUtil.hexStringToByteArray("0a2d808002b30309d")
        );
        Assert.assertTrue(errorMessage, ByteUtil.compare(a, b) == 1);
    }

    @Test
    public void testAsByteArray() throws Exception {
        byte[] bytes = randomBytes(random.nextInt(1024));
        byte[] result = ByteUtil.asByteArray(outputStream -> {
            outputStream.write(bytes);
        });
        Assert.assertArrayEquals(errorMessage, bytes, result);
    }

    @Test
    public void testBytesToHexString() throws Exception {
        byte[] b = bytes(0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef);
        Assert.assertEquals(errorMessage,
                "0123456789abcdef",
                ByteUtil.bytesToHexString(b));
    }

    @Test
    public void testHexStringToByteArray() throws Exception {
        Assert.assertArrayEquals(errorMessage,
                bytes(0xe0, 0x4f, 0xd0, 0x20, 0xea, 0x3a, 0x69, 0x10, 0xa2, 0xd8, 0x08, 0x00,
                        0x2b, 0x30, 0x030, 0x9d),
                assertPresent(
                        ByteUtil.hexStringToByteArray("e04fd020ea3a6910a2d808002b30309d")
                )
        );

        Assert.assertArrayEquals(errorMessage,
                bytes(),
                assertPresent(ByteUtil.hexStringToByteArray(""))
        );

        Assert.assertFalse(errorMessage,
                ByteUtil.hexStringToByteArray("notavalidhexstring").isPresent()
        );
    }
}
