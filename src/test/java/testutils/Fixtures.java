package testutils;

import com.google.inject.Inject;
import crypto.ECDSAPublicKey;
import server.access.KeyAccess;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static testutils.TestUtils.assertPresent;

public final class Fixtures {
    // all users have same password
    public static final String USER_PASSWORD = "34f3234917379d0ec59fbdaf897bd2aa58ae07f8339d41e3060000a5c4120013";

    private final UserAccess userAccess;
    private final KeyAccess keyAccess;

    @Inject
    public Fixtures(UserAccess userAccess, KeyAccess keyAccess) {
        this.userAccess = userAccess;
        this.keyAccess = keyAccess;
    }

    public User user(int userId) {
        try {
            return assertPresent(
                    String.format("No such user with id %d", userId),
                    userAccess.getUserByID(userId)
            );
        } catch (SQLException e) {
            fail(e.getMessage());
            throw new AssertionError();
        }
    }

    public Key keyOwnedBy(int userId) {
        try {
            List<Key> keys = keyAccess.getKeysByUserID(userId);
            assertFalse(String.format("User %d does not have any keys", userId), keys.isEmpty());
            return keys.get(0);
        } catch (SQLException e) {
            fail(e.getMessage());
            throw new AssertionError();
        }

    }

    public ECDSAPublicKey ecKeyOwnedBy(int userId) {
        return assertPresent(keyOwnedBy(userId).asKey());
    }
}
