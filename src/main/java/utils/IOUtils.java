package utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
}
