package testutils;

import crypto.Crypto;
import server.models.User;

import java.util.Base64;

public final class Fixtures {
    public static final String USER_PASSWORD = "g00dP@ssw0rd!!";
    public final User user;

    public Fixtures() throws Exception {
        byte[] salt = Base64.getDecoder().decode("m/g+zPZtEQIsWPLvjMoQCg==");
        byte[] passwordHash = Crypto.pbkdf2(USER_PASSWORD, salt);
        user = new User(1, "username", "example@example.com", salt, passwordHash, 0);
    }
}
