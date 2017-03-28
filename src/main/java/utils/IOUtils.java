package utils;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Various IO utilities
 */
public final class IOUtils {

    // Disallow instances of this class
    private IOUtils() { }

    /**
     * Fill the buffer with input from the input stream
     *
     * @param buffer buffer to fill
     * @throws IOException
     */
    public static void fill(InputStream inputStream, byte[] buffer) throws IOException {
        int index = 0;
        while (index < buffer.length) {
            int numBytes = inputStream.read(buffer, index, buffer.length - index);
            if (numBytes <= 0) {
                throw new IOException("Read EOF from input stream");
            }
            index += numBytes;
        }
    }

    /**
     * @param address String representation of socket address
     * @return The socket address that {@code address} represents,
     * if {@code address} is valid
     */
    public static Optional<InetSocketAddress> parseAddress(String address) {
        String[] pieces = address.split(":");
        if (pieces.length != 2) {
            return Optional.empty();
        }
        try {
            InetAddress addr = InetAddress.getByName(pieces[0]);
            int port = Integer.parseInt(pieces[1]);
            return Optional.of(new InetSocketAddress(addr, port));
        } catch (UnknownHostException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * @param path path to the file of socket addresses
     * @return list of socket addresses contained in the file at {@code path}
     * @throws IOException if there is an I/O error, or the file has misformatted
     *                     addresses
     */
    public static List<InetSocketAddress> parseAddresses(String path) throws IOException {
        try (BufferedReader nodeReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)
        )) {
            List<InetSocketAddress> result = new ArrayList<>();
            while (true) {
                String line = nodeReader.readLine();
                if (line == null || line.length() == 0) {
                    return result;
                }
                result.add(IOUtils.parseAddress(line).orElseThrow(() ->
                        new IOException(String.format("Invalid address: %s", line))
                ));
            }
        }
    }
}
