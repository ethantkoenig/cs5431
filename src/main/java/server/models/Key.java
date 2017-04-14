package server.models;


import java.util.Arrays;

public class Key {
    private final int userId;
    private final byte[] publicKey;
    public final String encryptedPrivateKey;

    public Key(int userId, byte[] publicKey, String encryptedPrivateKey) {
        this.userId = userId;
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public int getUserId() {
        return userId;
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }
}
