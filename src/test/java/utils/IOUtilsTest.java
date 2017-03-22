package utils;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    public void testParseAddress() throws IOException {
        Optional<InetSocketAddress> optAddr = IOUtils.parseAddress("localhost:9801");
        Assert.assertTrue(optAddr.isPresent());
        InetSocketAddress addr = optAddr.get();
        Assert.assertEquals(InetAddress.getByName("localhost"), addr.getAddress());
        Assert.assertEquals(9801, addr.getPort());

        optAddr = IOUtils.parseAddress("168.192.0.1:80");
        Assert.assertTrue(optAddr.isPresent());
        addr = optAddr.get();
        Assert.assertEquals(InetAddress.getByName("168.192.0.1"), addr.getAddress());
        Assert.assertEquals(80, addr.getPort());

        optAddr = IOUtils.parseAddress("not a valid address");
        Assert.assertFalse(optAddr.isPresent());

        optAddr = IOUtils.parseAddress("localhost:notANumber");
        Assert.assertFalse(optAddr.isPresent());
    }

    @Test
    public void testParseAddresses() throws IOException {
        File temp = File.createTempFile("test", ".tmp");
        TestUtils.writeFile(temp.getAbsolutePath(), "localhost:9801\n168.192.0.1:80\n");
        List<InetSocketAddress> addresses = IOUtils.parseAddresses(temp.getAbsolutePath());
        Assert.assertEquals(
                Arrays.asList(
                        new InetSocketAddress(InetAddress.getByName("localhost"), 9801),
                        new InetSocketAddress(InetAddress.getByName("168.192.0.1"), 80)
                ),
                addresses
        );

        TestUtils.writeFile(temp.getAbsolutePath(), "localhost:9801\nnotValid?:\n");
        try {
            IOUtils.parseAddresses(temp.getAbsolutePath());
            Assert.fail("Expected IOException");
        } catch (IOException e) {
            // success
        }
    }

}
