package server.bodies;

public class KeyBody {
    public final String publicKey;
    public final String encryptedPrivateKey;

    public KeyBody(String publicKey, String encryptedPrivateKey) {
        this.publicKey = publicKey;
        this.encryptedPrivateKey = encryptedPrivateKey;
    }
}
