package utils;

import org.bouncycastle.util.BigIntegers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public interface CanBeSerialized {

    /**
     * Writes the serialization of this object to {@code outputStream}
     *
     * @param outputStream output to write the serialized block to
     */
    void serialize(DataOutputStream outputStream) throws IOException;

    default void writeToDisk(File writeto) throws IOException {
        try (DataOutputStream writer = new DataOutputStream(new FileOutputStream(writeto))) {
            this.serialize(writer);
        }
    }

    static void serializeBytes(DataOutputStream outputStream, byte[] bytes)
            throws IOException {
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
    }

    static <U extends CanBeSerialized> void serializeArray(DataOutputStream outputStream,
                                                           U[] values)
            throws IOException {
        outputStream.writeInt(values.length);
        for (U value : values) {
            value.serialize(outputStream);
        }
    }

    static <U extends CanBeSerialized> void serializeList(DataOutputStream outputStream,
                                                          List<U> values)
            throws IOException {
        outputStream.writeInt(values.size());
        for (U value : values) {
            value.serialize(outputStream);
        }
    }

    static <U extends CanBeSerialized> void serializeSingleton(DataOutputStream outputStream,
                                                               U value)
            throws IOException {
        serializeList(outputStream, Collections.singletonList(value));
    }

    static void serializeUnsignedBigInteger(DataOutputStream outputStream, BigInteger value, int numBytes)
            throws IOException {
        byte[] bytes = BigIntegers.asUnsignedByteArray(numBytes, value);
        outputStream.write(bytes);
    }
}
