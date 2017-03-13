package server.models;


import java.util.Arrays;

public class Key {
    private final byte[] publicKey;
    private final byte[] encryptedPrivateKey;

    public Key(byte[] publicKey, byte[] encryptedPrivateKey) {
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.encryptedPrivateKey = Arrays.copyOf(encryptedPrivateKey, encryptedPrivateKey.length);
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }

    public byte[] getEncryptedPrivateKey() {
        return Arrays.copyOf(encryptedPrivateKey, encryptedPrivateKey.length);
    }

}
