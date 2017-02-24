package utils;

import java.io.IOException;
import java.io.OutputStream;
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

    public static ShaTwoFiftySix sha256(byte[] hash) {
        if (hash.length != HASH_SIZE_IN_BYTES) {
            // TODO report error somehow
            return null;
        }
        return new ShaTwoFiftySix(hash);
    }

    public byte[] copyOfHash() {
        return Arrays.copyOf(hash, HASH_SIZE_IN_BYTES);
    }

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
