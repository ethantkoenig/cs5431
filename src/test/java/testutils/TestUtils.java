package testutils;

import org.junit.Assert;
import utils.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Various (non-random) test utilities
 */
public class TestUtils {

    /**
     * @param port port number to use
     * @return a pair of sockets connected to each other
     * @throws IOException
     */
    public static Pair<Socket, Socket> sockets(int port)
            throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        Socket left = new Socket(InetAddress.getLocalHost(), port);
        Socket right = serverSocket.accept();
        return new Pair<>(left, right);
    }

    /**
     * Assert that {@code expected} and {@code actual} are {@code .equals()},
     * and that they have the same {@code .hashCode()}
     */
    public static void assertEqualsWithHashCode(String message, Object expected, Object actual) {
        Assert.assertEquals(message, expected, actual);
        Assert.assertEquals(
                String.format("hashCode() not equal for .equals() objects: %s", message),
                expected.hashCode(),
                actual.hashCode()
        );
    }
}
