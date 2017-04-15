package server.access;

import server.models.Key;
import server.models.User;
import server.utils.DbUtil;
import server.utils.Statements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The layer between user objects and the "users" table in the DB.
 * Utilities for reading and modifying database.
 */
public final class UserAccess {

    // Disallow instances of this class
    private UserAccess() {
    }

    private static final Logger LOGGER = Logger.getLogger(UserAccess.class.getName());

    /**
     * Given a username return the user object in the DB that is associated with this username
     * NOTE: we will need to not allow duplicate usernames.
     *
     * @param username the username of the user being queried
     * @throws SQLException
     */
    public static Optional<User> getUserbyUsername(String username) throws SQLException {
        try (
                Connection conn = DbUtil.getConnection(false);
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

    /**
     * Given an email return the user object in the DB that is associated with this username
     *
     * @param email the email of the user being queried
     * @throws SQLException
     */
    public static Optional<User> getUserbyEmail(String email) throws SQLException {
        try (
                Connection conn = DbUtil.getConnection(false);
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

    /**
     * @return The keys associated with a given userID
     */
    public static List<Key> getKeysByUserID(int userID) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.getKeysByUserID(conn, userID);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Key> keys = new ArrayList<>();
            while (rs.next()) {
                byte[] publicKeyBytes = rs.getBytes("publickey");
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                keys.add(new Key(publicKeyBytes, encryptedPrivateKeyBytes));
            }
            return keys;
        }
    }

    public static Key getKey(int userID, byte[] publicKey) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.getKey(conn, userID, publicKey);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                return new Key(publicKey, encryptedPrivateKeyBytes);
            }
            return null;
        }
    }

    /**
     * Add the given public/private keys to the database, under the given userID.
     */
    public static void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.insertKey(conn, userID, publicKey, privateKey)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }


    /**
     * Inserts a user into the users table in the yaccoin database
     */
    public static void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.insertUser(conn, username, email, salt, hashedPassword)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    /**
     * Updates a users password in the users table in the yaccoin database
     */
    public static void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.updateUserPassword(conn, userID, salt, hashedPassword)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    /**
     * Increments the failed login attempts associated with the given username
     */
    public static void incrementFailedLogins(String username) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.incrementFailedLogins(conn, username)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    /**
     * Resets the failed login attempts associated with the given username to 0
     */
    public static void resetFailedLogins(String username) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.resetFailedLogins(conn, username)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }
}
