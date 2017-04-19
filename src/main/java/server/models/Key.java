package server.models;


import crypto.ECDSAPublicKey;
import utils.DeserializationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class Key {
    private final int id;
    private final int userId;
    private final byte[] publicKey;
    public final String encryptedPrivateKey;

    public Key(int id, int userId, byte[] publicKey, String encryptedPrivateKey) {
        this.id = id;
        this.userId = userId;
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }

    public Optional<ECDSAPublicKey> asKey() {
        try {
            return Optional.of(ECDSAPublicKey.DESERIALIZER.deserialize(publicKey));
        } catch (IOException | DeserializationException e) {
            return Optional.empty();
        }
    }
}
