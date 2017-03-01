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

}
