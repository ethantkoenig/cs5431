package testutils;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import utils.ByteUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MockUserAccess implements UserAccess {
    private static volatile MockUserAccess INSTANCE = null;

    public final Fixtures fixtures;

    private final Set<User> users = new HashSet<>();
    private final Set<Key> keys = new HashSet<>();

    private MockUserAccess() throws Exception {
        fixtures = new Fixtures();
        users.add(fixtures.user);
        keys.add(fixtures.key);
    }

    public static void reset() {
        INSTANCE = null;
    }

    public static MockUserAccess get() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new MockUserAccess();
        }
        return INSTANCE;
    }

    @Override
    public List<String> getAllUsernames() throws SQLException{
        return null;
    }

    @Override
    public Optional<User> getUserbyUsername(String username) throws SQLException {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public Optional<User> getUserbyEmail(String email) throws SQLException {
        return users.stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public List<Key> getKeysByUserID(int userID) throws SQLException {
        return keys.stream().filter(k -> k.getUserId() == userID).collect(Collectors.toList());
    }

    @Override
    public Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException {
        return keys.stream().filter(k -> k.getUserId() == userID)
                .filter(k -> Arrays.equals(k.getPublicKey(), publicKey)).findFirst();
    }

    @Override
    public void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
        keys.add(new Key(userID, publicKey, privateKey));
    }

    @Override
    public void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
        int userId = users.stream().mapToInt(User::getId).max().orElse(0) + 1;
        users.add(new User(userId, username, email, salt, hashedPassword, 0));
    }

    @Override
    public void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        users.stream().filter(u -> u.getId() == userID).findFirst().ifPresent(u -> {
            users.remove(u);
            users.add(new User(userID, u.getUsername(), u.getEmail(), salt, hashedPassword, u.getFailedLogins()));
        });
    }

    @Override
    public void incrementFailedLogins(int userID) throws SQLException {
        users.stream().filter(u -> u.getId() == (userID)).findFirst().ifPresent(u -> {
            users.remove(u);
            users.add(new User(u.getId(), u.getUsername(), u.getEmail(),
                    u.getSalt(), u.getHashedPassword(), u.getFailedLogins() + 1));
        });
    }

    @Override
    public void resetFailedLogins(int userID) throws SQLException {
        users.stream().filter(u -> u.getId() == (userID)).findFirst().ifPresent(u -> {
            users.remove(u);
            users.add(new User(u.getId(), u.getUsername(), u.getEmail(),
                    u.getSalt(), u.getHashedPassword(), 0));
        });
    }

    @Override
    public boolean isFriendsWith(String username, String friend) throws SQLException{
        return true;
    }

    @Override
    public void insertFriends(String username, String friend) throws SQLException{

    }

    @Override
    public void deleteFriends(String username, String friend) throws SQLException{

    }

    @Override
    public List<String> getFriends(String username) throws SQLException{
        return null;
    }

    @Override
    public List<String> getPeopleWhoFriendMe(String username) throws SQLException{
        return null;
    }

    public static final class Fixtures {
        public static final String USER_PASSWORD = "g00dP@ssw0rd!!";
        public final User user;

        public final ECDSAPrivateKey PRIVATE_KEY;
        public final Key key;

        private Fixtures() throws Exception {
            byte[] salt = Crypto.generateSalt();
            byte[] passwordHash = Crypto.pbkdf2(USER_PASSWORD, salt);
            user = new User(1, "username", "example@example.com", salt, passwordHash, 0);

            ECDSAKeyPair keyPair = Crypto.signatureKeyPair();
            PRIVATE_KEY = keyPair.privateKey;
            key = new Key(user.getId(),
                    ByteUtil.asByteArray(keyPair.publicKey::serialize),
                    ByteUtil.bytesToHexString(ByteUtil.asByteArray(PRIVATE_KEY::serialize)));
        }
    }

    public static final class Model extends AbstractModule {
        @Override
        protected void configure() {
            bind(UserAccess.class).to(MockUserAccess.class);
        }

        @Provides
        MockUserAccess provideMockUserAccess() throws Exception {
            return MockUserAccess.get();
        }
    }
}
