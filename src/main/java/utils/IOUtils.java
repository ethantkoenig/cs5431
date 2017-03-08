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
public class IOUtils {

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

    public static void sendMessage(DataOutputStream outputStream, byte type, byte[] payload)
            throws IOException {
        outputStream.writeInt(payload.length);
        outputStream.write(type);
        outputStream.write(payload);
    }

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
