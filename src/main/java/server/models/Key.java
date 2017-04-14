package server.models;


import java.util.Arrays;

public class Key {
    private final byte[] publicKey;
    public final String encryptedPrivateKey;

    public Key(byte[] publicKey, String encryptedPrivateKey) {
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }
}
