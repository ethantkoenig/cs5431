package utils;

import java.io.IOException;
import java.io.InputStream;

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

    public static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] parseHex(String hexString) {
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("hex string must be of even length");
        }
        byte[] data = new byte[hexString.length() / 2];
        for (int i = 0; i < data.length; i++) {
            int n = 16 * Character.digit(hexString.charAt(2 * i), 16)
                    + Character.digit(hexString.charAt(2 * i + 1), 16);
            data[i] = (byte) n;
        }
        return data;
    }
}
