package utils;

import org.bouncycastle.util.BigIntegers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public interface Deserializer<T> {
    int DEFAULT_MAX_LIST_LENGTH = 128;

    T deserialize(DataInputStream inputStream)
            throws DeserializationException, IOException;

    default T deserialize(byte[] inputBytes)
            throws DeserializationException, IOException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(inputBytes)));
    }

    static <U> List<U> deserializeList(byte[] inputBytes,
                                       Deserializer<U> deserializer)
            throws DeserializationException, IOException {
        InputStream inputStream = new ByteArrayInputStream(inputBytes);
        return deserializeList(new DataInputStream(inputStream), deserializer);
    }

    static <U> List<U> deserializeList(byte[] inputBytes,
                                       Deserializer<U> deserializer,
                                       int maxLength)
            throws DeserializationException, IOException {
        InputStream inputStream = new ByteArrayInputStream(inputBytes);
        return deserializeList(new DataInputStream(inputStream), deserializer, maxLength);
    }

    static <U> List<U> deserializeList(DataInputStream inputStream,
                                       Deserializer<U> deserializer)
            throws DeserializationException, IOException {
        return deserializeList(inputStream, deserializer, DEFAULT_MAX_LIST_LENGTH);
    }

    static <U> List<U> deserializeList(DataInputStream inputStream,
                                       Deserializer<U> deserializer,
                                       int maxLength)
            throws DeserializationException, IOException {
        int length = inputStream.readInt();
        if (length < 0 || length >= maxLength) {
            String msg = String.format("Invalid list length: %d", length);
            throw new DeserializationException(msg);
        }
        List<U> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            result.add(deserializer.deserialize(inputStream));
        }
        return result;
    }

    static byte[] deserializeBytes(DataInputStream inputStream,
                                   int maxLength)
            throws DeserializationException, IOException {
        int length = inputStream.readInt();
        if (length < 0 || length >= maxLength) {
            String msg = String.format("Invalid byte array length: %d", length);
            throw new DeserializationException(msg);
        }
        byte[] bytes = new byte[length];
        IOUtils.fill(inputStream, bytes);
        return bytes;
    }

    static BigInteger deserializeUnsignedBigInteger(DataInputStream inputStream, int numBytes)
            throws DeserializationException, IOException {
        byte[] bytes = new byte[numBytes];
        IOUtils.fill(inputStream, bytes);
        return BigIntegers.fromUnsignedByteArray(bytes);
    }
}
