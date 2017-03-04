package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.io.*;
import java.nio.ByteBuffer;
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

    @Test
    public void testSendMessage() throws IOException {
        byte[] payload = randomBytes(random.nextInt(1024));
        byte type = (byte) random.nextInt(Byte.MAX_VALUE + 1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        IOUtils.sendMessage(new DataOutputStream(outputStream), type, payload);

        byte[] written = outputStream.toByteArray();
        Assert.assertEquals(errorMessage,
                written.length, Integer.BYTES + Byte.BYTES + payload.length);

        ByteBuffer buffer = ByteBuffer.wrap(written);
        int writtenLen = buffer.getInt();
        byte writtenType = buffer.get();
        byte[] writtenPayload = new byte[payload.length];
        buffer.get(writtenPayload);

        Assert.assertEquals(errorMessage, payload.length, writtenLen);
        Assert.assertEquals(errorMessage, type, writtenType);
        Assert.assertArrayEquals(errorMessage, payload, writtenPayload);
    }
}
