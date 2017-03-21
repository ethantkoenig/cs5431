package server.access;

import server.models.Key;
import server.models.User;
import server.utils.DbUtil;
import server.utils.Statements;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The layer between user objects and the "users" table in the DB.
 * Utilities for reading and modifying database.
 */
public class UserAccess {

    private static final Logger LOGGER = Logger.getLogger(UserAccess.class.getName());

    /**
     * Given a username return the user object in the DB that is associated with this username
     * NOTE: we will need to not allow duplicate usernames.
     *
     * @param username the username of the user being queried
     * @throws SQLException
     */
    public static User getUserbyUsername(String username) throws SQLException {
        try (
                Connection conn = DbUtil.getConnection(false);
                PreparedStatement preparedStmt = Statements.selectUserByUsername(conn, username);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int id = rs.getInt("id");
                byte[] salt = rs.getBytes("salt");
                byte[] hashedPassword = rs.getBytes("pass");
                return new User(id, username, salt, hashedPassword);
            }
            return null;
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
                byte[] encryptedPrivateKeyBytes = rs.getBytes("privatekey");
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
                byte[] encryptedPrivateKeyBytes = rs.getBytes("privatekey");
                return new Key(publicKey, encryptedPrivateKeyBytes);
            }
            return null;
        }
    }

    /**
     * Add the given public/private keys to the database, under the given userID.
     */
    public static void insertKey(int userID, byte[] publicKey, byte[] privateKey) throws SQLException {
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
    public static void insertUser(String username, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.insertUser(conn, username, salt, hashedPassword)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }
}
