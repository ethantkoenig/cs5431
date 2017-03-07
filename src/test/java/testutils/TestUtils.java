package testutils;

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
}
