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
public final class DatabaseUserAccess implements UserAccess {

    public DatabaseUserAccess() {
    }

    private static final Logger LOGGER = Logger.getLogger(DatabaseUserAccess.class.getName());

    @Override
    public Optional<User> getUserbyUsername(String username) throws SQLException {
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
    @Override
    public Optional<User> getUserbyEmail(String email) throws SQLException {
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

    @Override
    public List<Key> getKeysByUserID(int userID) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.getKeysByUserID(conn, userID);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Key> keys = new ArrayList<>();
            while (rs.next()) {
                byte[] publicKeyBytes = rs.getBytes("publickey");
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                keys.add(new Key(userID, publicKeyBytes, encryptedPrivateKeyBytes));
            }
            return keys;
        }
    }

    public Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.getKey(conn, userID, publicKey);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                String encryptedPrivateKeyBytes = rs.getString("privatekey");
                return Optional.of(new Key(userID, publicKey, encryptedPrivateKeyBytes));
            }
            return Optional.empty();
        }
    }

    @Override
    public void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
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

    @Override
    public void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
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

    @Override
    public void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
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

    @Override
    public void incrementFailedLogins(String username) throws SQLException {
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

    @Override
    public void resetFailedLogins(String username) throws SQLException {
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
