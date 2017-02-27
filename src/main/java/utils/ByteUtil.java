package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Byte Manipulation functions
 */
public class ByteUtil {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static void addOne(byte[] b) throws Exception {
        for (int i = b.length - 1; i >= 0; i--) {
            if (b[i] < 126) {
                b[i]++;
                return;
            }
            b[i] = 0;
            if (i == 0) {
                throw new Exception("Overflow");
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

    /***********************************************************************************************************
     * The following byte to hex string function will most likely not be used but we'll keep them around for now.
     * Useful for debugging and logging.
     **********************************************************************************************************/

    // Credit: http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int length = s.length();
        if (length % 2 != 0) {
            length++;
            s = "0" + s;
        }
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) (Character.digit(s.charAt(i), 16) << 4);
            byteArray[i / 2] += (byte) Character.digit(s.charAt(i + 1), 16);
        }
        return byteArray;
    }

    public static String addOne(String hex) {
        BigInteger bi = new BigInteger(hex, 16);
        bi = bi.add(BigInteger.ONE);
        return bi.toString(16);
    }


}
