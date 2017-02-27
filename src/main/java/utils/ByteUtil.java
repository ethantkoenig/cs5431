package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Byte Manipulation functions
 * TODO: getting rid of the shitting string to byte array conversion bullshit. Will just do all byte array
 */
public class ByteUtil {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

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

    public static byte[] addOne(byte[] b) {
        String bytes = bytesToHexString(b);
        BigInteger value = new BigInteger(bytes, 16);
        value = value.add(BigInteger.ONE);
        return hexStringToByteArray(value.toString(16));
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
        return (bytesToHexString(a).compareTo(bytesToHexString(b)) > 0) ? 1 : -1;
    }

}
