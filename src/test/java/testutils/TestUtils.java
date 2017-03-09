package testutils;

import org.junit.Assert;
import utils.Pair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

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

    /**
     * Write to a file, and assert that there are no errors.
     *
     * @param path     path of file to write
     * @param contents contents to write
     */
    public static void writeFile(String path, String contents) {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8
        )) {
            writer.write(contents);
        } catch (IOException e) {
            Assert.fail(String.format("Unexpected exception while writing file: %s", e.getMessage()));
        }
    }

    /**
     * Run {@code thrower} and assert that it {@code throws} a {@code Throwable} that is an instance of
     * {@code throwableClass}.
     *
     * @param errorMessage   The message to print if {@code thrower} either does not throw or {@code throws} that wrong
     *                       kind of {@code Throwable}.
     * @param thrower        The function to test
     * @param throwableClass The {@code Class object} corresponding to the intended {@code Class} of the
     *                       {@code Throwable} to be thrown.
     */
    public static <T extends Throwable> void assertThrows(String errorMessage, RunnableThrower thrower, Class<T> throwableClass) {
        try {
            thrower.run();
            Assert.fail(errorMessage);
        } catch (Throwable e) {
            Assert.assertTrue(errorMessage, throwableClass.isInstance(e));
        }
    }

    @FunctionalInterface
    public interface RunnableThrower {
        void run() throws Throwable;
    }

}
