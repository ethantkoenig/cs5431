package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Byte Manipulation functions
 */
public class ByteUtil {

    // concatenate two byte arrays
    public static byte[] concatenate(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);
        return outputStream.toByteArray();
    }

    public static byte[] addOne(byte[] a) {
        for (int i = a.length - 1; i >= 0; --i) {
            if (++a[i] != 0) {
                return a;
            }
        }
        throw new IllegalStateException("Counter overflow");
    }
}
