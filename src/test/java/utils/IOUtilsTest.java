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

}
