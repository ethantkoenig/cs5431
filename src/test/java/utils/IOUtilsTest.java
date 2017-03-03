package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IOUtilsTest extends RandomizedTest {

    @Test
    public void testFillComplete() throws IOException {
        byte[] content = randomBytes(random.nextInt(1024));
        InputStream inputStream = new ByteArrayInputStream(content);

        byte[] dest = new byte[content.length];
        IOUtils.fill(inputStream, dest);
        Assert.assertArrayEquals(errorMessage, dest, content);
    }

    @Test
    public void testFillPartial() throws IOException {
        byte[] content = randomBytes(1 + random.nextInt(1024));
        InputStream inputStream = new ByteArrayInputStream(content);

        int index = 0;
        while (index < content.length) {
            byte[] dest = new byte[random.nextInt(1 + content.length - index)];
            IOUtils.fill(inputStream, dest);
            Assert.assertArrayEquals(errorMessage, dest,
                    Arrays.copyOfRange(content, index, index + dest.length));
            index += dest.length;
        }
    }

    @Test(expected = IOException.class)
    public void testFillUnderflow() throws IOException {
        byte[] content = randomBytes(random.nextInt(1024));
        InputStream inputStream = new ByteArrayInputStream(content);

        byte[] dest = new byte[content.length + 1];
        IOUtils.fill(inputStream, dest);
    }

    @Test
    public void testToHex() {
        byte[] bytes = new byte[]{0x00, 0x34, (byte) 0xab};
        Assert.assertEquals("0034ab", IOUtils.toHex(bytes));
    }

    @Test
    public void testParseHex() {
        byte[] bytes = IOUtils.parseHex("0034ab");
        Assert.assertArrayEquals(bytes, new byte[]{0x00, 0x34, (byte) 0xab});
    }

    @Test
    public void testHex() {
        byte[] bytes = randomBytes(2 * random.nextInt(128));
        String hex = IOUtils.toHex(bytes);
        byte[] parsedBytes = IOUtils.parseHex(hex);
        Assert.assertArrayEquals(errorMessage, bytes, parsedBytes);
    }
}
