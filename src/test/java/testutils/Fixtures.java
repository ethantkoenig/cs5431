package testutils;

import crypto.Crypto;
import crypto.ECDSAPublicKey;
import server.models.User;
import utils.DeserializationException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;

public final class Fixtures {
    public static final String USER_PASSWORD = "g00dP@ssw0rd!!";
    public final User user;

    public final ECDSAPublicKey key;

    public Fixtures() {
        try {
            byte[] salt = Base64.getDecoder().decode("m/g+zPZtEQIsWPLvjMoQCg==");
            byte[] passwordHash = Crypto.pbkdf2(USER_PASSWORD, salt);
            user = new User(1, "username", "example@example.com", salt, passwordHash, 0);

            byte[] keyBytes = Base64.getDecoder()
                    .decode("Q2Wpo3zjP9wplEpXdTLceXeVvj1HTHZHdQbD4fg1Ttg9gPYbRRRxJc4AL0Dkt2bWnQVIYECUCJEb80lNhDPKSg==");
            key = ECDSAPublicKey.DESERIALIZER.deserialize(keyBytes);
        } catch (DeserializationException | GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
