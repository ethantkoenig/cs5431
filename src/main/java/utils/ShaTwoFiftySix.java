package utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Represents a SHA-256 hash
 */
public final class ShaTwoFiftySix {
    public static final int HASH_SIZE_IN_BYTES = 32;

    private final byte[] hash;

    private ShaTwoFiftySix(byte[] hash) {
        this.hash = hash;
    }

    /**
     * Reads a SHA-256 hash from {@code input}, and returns the corresponding
     * {@code ShaTwoFiftySix} object
     *
     * @param input input bytes containing SHA-256 hash
     * @return A {@code ShaTwoFiftySix} object corresponding to the read SHA-256 hash
     * @throws BufferUnderflowException
     */
    public static ShaTwoFiftySix deserialize(ByteBuffer input) throws BufferUnderflowException {
        byte[] hash = new byte[ShaTwoFiftySix.HASH_SIZE_IN_BYTES];
        input.get(hash);
        return new ShaTwoFiftySix(hash);
    }

    public static ShaTwoFiftySix create(byte[] hash) {
        if (hash.length != HASH_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Mis-sized SHA-256 hash");
        }
        return new ShaTwoFiftySix(Arrays.copyOf(hash, HASH_SIZE_IN_BYTES));
    }

    /**
     * Hashes {@code content}, and returns a corresponding {@code ShaTwoFiftySix} object
     *
     * @param content sequence of bytes to hash
     * @return A {@code ShaTwoFiftySix} object corresponding to the hash of {@code content}
     */
    public static ShaTwoFiftySix hashOf(byte[] content) throws GeneralSecurityException {
        byte[] hash = Crypto.sha256(content);
        if (hash.length != HASH_SIZE_IN_BYTES) {
            String msg = String.format("Unexpected length of SHA-256 hash: %d", hash.length);
            throw new GeneralSecurityException(msg);
        }
        return new ShaTwoFiftySix(hash);
    }

    /**
     * @return A copy of the hash
     */
    public byte[] copyOfHash() {
        return Arrays.copyOf(hash, HASH_SIZE_IN_BYTES);
    }

    /**
     * Writes the hash to {@code outputStream}.
     *
     * @param outputStream {@code OutputStream} to write the hash to
     * @throws IOException
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(hash);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof ShaTwoFiftySix)) {
            return false;
        }
        ShaTwoFiftySix other = (ShaTwoFiftySix) o;
        return Arrays.equals(hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }
}
