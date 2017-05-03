package server.access;

import com.google.inject.Inject;
import server.models.Key;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The layer between user objects and the "users" table in the DB.
 * Utilities for reading and modifying database.
 */
public final class DatabaseUserAccess implements UserAccess {
    private final ConnectionProvider connectionProvider;

    @Inject
    public DatabaseUserAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private static final Logger LOGGER = Logger.getLogger(DatabaseUserAccess.class.getName());

    @Override
    public List<String> getAllUsernames() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(Statements.GET_ALL_USERS)
        ) {
            List<String> users = new ArrayList<>();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            return users;
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws SQLException {
        try (
                Connection conn = connectionProvider.getConnection();
                PreparedStatement preparedStmt = Statements.selectUserByUsername(conn, username);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int id = rs.getInt("id");
                byte[] salt = rs.getBytes("salt");
                byte[] hashedPassword = rs.getBytes("pass");
                String email = rs.getString("email");
                int failedLogins = rs.getInt("failedLogins");
                return Optional.of(new User(id, username, email, salt, hashedPassword, failedLogins));
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> getUserByEmail(String email) throws SQLException {
        try (
                Connection conn = connectionProvider.getConnection();
                PreparedStatement preparedStmt = Statements.selectUserByEmail(conn, email);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int id = rs.getInt("id");
                byte[] salt = rs.getBytes("salt");
                byte[] hashedPassword = rs.getBytes("pass");
                String username = rs.getString("username");
                int failedLogins = rs.getInt("failedLogins");
                return Optional.of(new User(id, username, email, salt, hashedPassword, failedLogins));
            }
            return Optional.empty();
        }
    }

    @Override
    public List<Key> getKeysByUserID(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getKeysByUserID(conn, userID);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Key> keys = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("keypairid");
                byte[] publicKeyBytes = rs.getBytes("publickey");
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                keys.add(new Key(id, userID, publicKeyBytes, encryptedPrivateKeyBytes));
            }
            return keys;
        }
    }

    public Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getKey(conn, userID, publicKey);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int id = rs.getInt("keypairid");
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                return Optional.of(new Key(id, userID, publicKey, encryptedPrivateKeyBytes));
            }
            return Optional.empty();
        }
    }

    @Override
    public void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertKey(conn, userID, publicKey, privateKey)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void deleteKey(int keyID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteKey(conn, keyID)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Delete affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void deleteAllKeys(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteAllKeys(conn, userID)) {
            preparedStmt.executeUpdate();
        }
    }

    @Override
    public void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertUser(conn, username, email, salt, hashedPassword)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.updateUserPassword(conn, userID, salt, hashedPassword)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void incrementFailedLogins(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.incrementFailedLogins(conn, userID)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void resetFailedLogins(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.resetFailedLogins(conn, userID)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public boolean isFriendsWith(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.isFriendsWith(conn, username, friend);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void insertFriends(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertFriends(conn, username, friend)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public void deleteFriends(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteFriends(conn, username, friend)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public List<String> getFriends(String username) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getFriends(conn, username);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<String> friends = new ArrayList<>();
            while (rs.next()) {
                friends.add(rs.getString("friend"));
            }
            return friends;
        }
    }

    @Override
    public List<String> getPeopleWhoFriendMe(String username) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPeopleWhoFriendMe(conn, username);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<String> usernames = new ArrayList<>();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            return usernames;
        }
    }

    @Override
    public void insertPendingKey(int userid, byte[] publickey, String privatekey, String guidhash) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertPendingKeyPair(conn, userid, publickey, privatekey, guidhash);
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    @Override
    public Optional<Key> lookupPendingKey(String guidhash) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPendingKeyByGuid(conn, guidhash);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (!rs.next()) return Optional.empty();
            int id = rs.getInt("userid");
            byte[] publicKeyBytes = rs.getBytes("publickey");
            String encryptedPrivateKeyBytes = rs.getString("privatekey");
            return Optional.of(new Key(-1, id, publicKeyBytes, encryptedPrivateKeyBytes));
        }

    }

    @Override
    public void removePendingKey(String guidhash) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deletePendingKey(conn, guidhash);
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }
}
