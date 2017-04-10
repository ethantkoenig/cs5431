package crypto;

public class ECDSAKeyPair {
    public final ECDSAPrivateKey privateKey;
    public final ECDSAPublicKey publicKey;

    public ECDSAKeyPair(ECDSAPrivateKey privateKey, ECDSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
}
