package server.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Statements {

    // Disallow instances of this class
    private Statements() {
    }

    // Update statements
    public static final String DB_NAME = "yaccoin";
    public static final String CREATE_DB = "CREATE DATABASE yaccoin";
    public static final String USE_DB = "USE yaccoin";
    public static final String CREATE_USERS_TABLE = "CREATE TABLE users ("
            + "id int NOT NULL AUTO_INCREMENT,"
            + "username varchar(32) NOT NULL,"
            + "email varchar(128) NOT NULL,"
            + "salt varbinary(32) NOT NULL,"
            + "pass varbinary(2048) NOT NULL,"
            + "failedLogins int NOT NULL DEFAULT 0,"
            + "PRIMARY KEY (id),"
            + "UNIQUE (username),"
            + "UNIQUE (email)"
            + ")";
    public static final String CREATE_KEYS_TABLE = "CREATE TABLE keypairs ("
            + "keypairid int NOT NULL AUTO_INCREMENT,"
            + "userid int NOT NULL,"
            + "publickey varbinary(91) NOT NULL,"
            + "privatekey text NOT NULL,"
            + "PRIMARY KEY (keypairid),"
            + "INDEX userid_index (userid),"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(id)"
            + "  ON DELETE CASCADE"
            + ")";
    public static final String CREATE_PASSWORD_RECOVERY_TABLE = "CREATE TABLE passrecover ("
            + "userid int NOT NULL,"
            + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "guidhash varchar(2048) NOT NULL,"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(id)"
            + "  ON DELETE CASCADE"
            + ")";
    public static final String CREATE_FRIENDS_TABLE = "CREATE TABLE friends ("
            + "username varchar(32) NOT NULL,"
            + "friend varchar(32) NOT NULL"
            + ")";
    public static final String CREATE_PENDING_KEYS_TABLE = "CREATE TABLE pendingkeys ("
            + "userid int NOT NULL,"
            + "publickey varbinary(91) NOT NULL,"
            + "privatekey text NOT NULL,"
            + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "guidhash varchar(2048) NOT NULL,"
            + "INDEX userid_index (userid),"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(id)"
            + "  ON DELETE CASCADE"
            + ")";
    public static final String SHOW_DB_LIKE = String.format("SHOW DATABASES LIKE '%s'", DB_NAME);
    public static final String GET_ALL_USERS = "SELECT * FROM users";
    private static final int RECOVERY_TIME = 60 * 60; // 1 hour for recovery link to remain active


    @FunctionalInterface
    public interface Populator {
        void populate(PreparedStatement statement) throws SQLException;
    }

    private static PreparedStatement prepareStatement(PreparedStatement statement,
                                                      Populator populator) throws SQLException {
        try {
            populator.populate(statement);
            return statement;
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    public static PreparedStatement selectUserByUsername(Connection connection, String username)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?"),
                statement -> statement.setString(1, username)
        );
    }

    public static PreparedStatement selectUserByEmail(Connection connection, String email)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM users WHERE email = ?"),
                statement -> statement.setString(1, email)
        );
    }

    public static PreparedStatement getKeysByUserID(Connection connection, int userID)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM keypairs WHERE userid = ?"),
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatement getKey(Connection connection, int userID, byte[] publicKey)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM keypairs WHERE userid = ? AND publickey = ?"),
                statement -> {
                    statement.setInt(1, userID);
                    statement.setBytes(2, publicKey);
                }
        );
    }

    public static PreparedStatement insertUser(Connection connection,
                                               String username,
                                               String email,
                                               byte[] salt,
                                               byte[] hashedPassword) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO users (username, email, salt, pass) VALUES (?, ?, ?, ?)"),
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, email);
                    statement.setBytes(3, salt);
                    statement.setBytes(4, hashedPassword);
                }
        );
    }

    public static PreparedStatement insertKey(Connection connection,
                                              int userID,
                                              byte[] publicKey,
                                              String privateKey) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO keypairs (userid, publickey, privatekey) VALUES (?, ?, ?)"),
                statement -> {
                    statement.setInt(1, userID);
                    statement.setBytes(2, publicKey);
                    statement.setString(3, privateKey);
                }
        );
    }

    public static PreparedStatement deleteKey(Connection connection,
                                              int keyID) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "DELETE FROM keypairs WHERE keypairid = ?"),
                statement -> statement.setInt(1, keyID)
        );
    }

    public static PreparedStatement deleteAllKeys(Connection connection,
                                                  int userID) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "DELETE FROM keypairs WHERE userid = ?"),
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatement getPasswordRecoveryUserID(Connection connection, String GUIDHash)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM passrecover WHERE guidhash=? and dt BETWEEN (NOW() - INTERVAL " + RECOVERY_TIME + " SECOND) AND NOW()"),
                statement -> statement.setString(1, GUIDHash)
        );
    }

    public static PreparedStatement insertPasswordRecovery(Connection connection, int userID, String GUIDHash) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO passrecover (userid, guidhash) VALUES (?, ?)"),
                statement -> {
                    statement.setInt(1, userID);
                    statement.setString(2, GUIDHash);
                }
        );
    }

    public static PreparedStatement updateUserPassword(Connection connection, int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "UPDATE users SET pass = ?, salt = ? WHERE id = ?"),
                statement -> {
                    statement.setBytes(1, hashedPassword);
                    statement.setBytes(2, salt);
                    statement.setInt(3, userID);
                }
        );
    }

    public static PreparedStatement incrementFailedLogins(Connection connection, int userID) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "UPDATE users SET failedLogins = failedLogins + 1 WHERE id = ?"),
                statement -> {
                    statement.setInt(1, userID);
                }
        );
    }

    public static PreparedStatement resetFailedLogins(Connection connection, int userID) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "UPDATE users SET failedLogins = 0 WHERE id = ?"),
                statement -> {
                    statement.setInt(1, userID);
                }
        );
    }

    public static PreparedStatement isFriendsWith(Connection connection, String username, String friend) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM friends WHERE username = ? AND friend = ?"),
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatement insertFriends(Connection connection, String username, String friend) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO friends (username, friend) VALUES (?, ?)"),
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatement deleteFriends(Connection connection, String username, String friend) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "DELETE FROM friends WHERE username = ? AND friend = ?"),
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatement getFriends(Connection connection, String username) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT friend FROM friends WHERE username = ?"),
                statement -> {
                    statement.setString(1, username);
                }
        );
    }

    public static PreparedStatement getPeopleWhoFriendMe(Connection connection, String username) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT username FROM friends WHERE friend = ?"),
                statement -> {
                    statement.setString(1, username);
                }
        );
    }

    public static PreparedStatement insertPendingKeyPair(Connection connection, int userid, byte[] publickey,
                                                         String privatekey, String guidhash) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO pendingkeys (userid, publickey, privatekey, guidhash) VALUES (?, ?, ?, ?)"),
                statement -> {
                    statement.setInt(1, userid);
                    statement.setBytes(2, publickey);
                    statement.setString(3, privatekey);
                    statement.setString(4, guidhash);
                }
        );
    }

    public static PreparedStatement getPendingKeyByGuid(Connection connection, String guidhash) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM pendingkeys WHERE guidhash = ?"),
                statement -> {
                    statement.setString(1, guidhash);
                }
        );
    }

    public static PreparedStatement deletePendingKey(Connection connection, String guidhash) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "DELETE FROM pendingkeys WHERE guidhash = ?"),
                statement -> {
                    statement.setString(1, guidhash);
                }
        );
    }

}
