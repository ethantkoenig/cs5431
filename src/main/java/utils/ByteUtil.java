package utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Optional;

/**
 * Byte Manipulation functions
 */
public final class ByteUtil {

    // Disallow instances of this class
    private ByteUtil() {
    }

    public static void addOne(byte[] b) {
        for (int i = b.length - 1; i >= 0; i--) {
            if (++b[i] != 0) {
                return;
            }
        }
    }

    // concatenate two byte arrays
    public static byte[] concatenate(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);
        return outputStream.toByteArray();
    }

    public static int compare(byte[] a, byte[] b) {
        if (Arrays.equals(a, b)) return 0;
        BigInteger bia = new BigInteger(a);
        BigInteger bib = new BigInteger(b);
        return bia.compareTo(bib);
    }

    public static <T extends Exception> byte[] forceByteArray(Serializer<T> serializer) {
        try {
            return asByteArray(serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Exception> byte[] asByteArray(Serializer<T> serializer)
            throws T {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serializer.serialize(new DataOutputStream(outputStream));
        return outputStream.toByteArray();
    }

    @FunctionalInterface
    public interface Serializer<T extends Exception> {
        void serialize(DataOutputStream outputStream) throws T;
    }

    /***********************************************************************************************************
     * The following byte to hex string function will most likely not be used but we'll keep them around for now.
     * Useful for debugging and logging.
     **********************************************************************************************************/

    public static String bytesToHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static Optional<byte[]> hexStringToByteArray(String s) {
        int length = s.length();
        if (length % 2 != 0) {
            length++;
            s = "0" + s;
        }
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                return Optional.empty();
            }
            byteArray[i / 2] = (byte) ((hi << 4) + lo);
        }
        return Optional.of(byteArray);
    }
}
