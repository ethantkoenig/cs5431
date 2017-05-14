package server.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Statements {

    // Disallow instances of this class
    private Statements() {
    }

    // Update statements
    public static final String DB_NAME = "ezracoinl";
    public static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS users ("
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
    public static final String CREATE_KEYS_TABLE = "CREATE TABLE IF NOT EXISTS keypairs ("
            + "keypairid int NOT NULL AUTO_INCREMENT,"
            + "userid int NOT NULL,"
            + "publickey varbinary(91) NOT NULL,"
            + "privatekey text NOT NULL,"
            + "PRIMARY KEY (keypairid),"
            + "INDEX userid_index (userid),"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(id)"
            + "  ON DELETE CASCADE"
            + "UNIQUE (userid, publickey)"
            + ")";
    public static final String CREATE_PASSWORD_RECOVERY_TABLE = "CREATE TABLE IF NOT EXISTS recover ("
            + "userid int NOT NULL,"
            + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "guidhash varchar(2048) NOT NULL,"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(id)"
            + "  ON DELETE CASCADE"
            + ")";
    public static final String CREATE_FRIENDS_TABLE = "CREATE TABLE IF NOT EXISTS friends ("
            + "username varchar(32) NOT NULL,"
            + "friend varchar(32) NOT NULL"
            + ")";

    public static final String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE IF NOT EXISTS transactions ("
            + "tranid int NOT NULL AUTO_INCREMENT,"
            + "fromuser varchar(32) NOT NULL,"
            + "touser varchar(32) NOT NULL,"
            + "amount bigint NOT NULL,"
            + "message varchar(256),"
            + "isrequest boolean not null default 0,"
            + "PRIMARY KEY (tranid)"
            + ")";

    public static final String CREATE_PENDING_KEYS_TABLE = "CREATE TABLE IF NOT EXISTS pendingkeys ("
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

    public static final String GET_ALL_USERS = "SELECT * FROM users";
    private static final int RECOVERY_TIME = 60 * 60; // 1 hour for recovery link to remain active


    @FunctionalInterface
    public interface Populator {
        void populate(PreparedStatement statement) throws SQLException;
    }

    private static PreparedStatementProvider provider(String query,
                                                      Populator populator) throws SQLException {
        return conn -> {
            PreparedStatement statement = conn.prepareStatement(query);
            try {
                populator.populate(statement);
                return statement;
            } catch (SQLException e) {
                statement.close();
                throw e;
            }
        };
    }

    public static PreparedStatementProvider selectUserByID(int userID)
            throws SQLException {
        return provider("SELECT * FROM users WHERE id = ?",
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatementProvider selectUserByUsername(String username)
            throws SQLException {
        return provider("SELECT * FROM users WHERE username = ?",
                statement -> statement.setString(1, username)
        );
    }

    public static PreparedStatementProvider selectUserByEmail(String email)
            throws SQLException {
        return provider("SELECT * FROM users WHERE email = ?",
                statement -> statement.setString(1, email)
        );
    }

    public static PreparedStatementProvider getKeysByUserID(int userID)
            throws SQLException {
        return provider("SELECT * FROM keypairs WHERE userid = ?",
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatementProvider getKey(int userID, byte[] publicKey)
            throws SQLException {
        return provider("SELECT * FROM keypairs WHERE userid = ? AND publickey = ?",
                statement -> {
                    statement.setInt(1, userID);
                    statement.setBytes(2, publicKey);
                }
        );
    }

    public static PreparedStatementProvider insertUser(String username,
                                                       String email,
                                                       byte[] salt,
                                                       byte[] hashedPassword) throws SQLException {
        return provider("INSERT INTO users (username, email, salt, pass) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, email);
                    statement.setBytes(3, salt);
                    statement.setBytes(4, hashedPassword);
                }
        );
    }

    public static PreparedStatementProvider insertKey(
            int userID,
            byte[] publicKey,
            String privateKey) throws SQLException {
        return provider("INSERT INTO keypairs (userid, publickey, privatekey) VALUES (?, ?, ?)",
                statement -> {
                    statement.setInt(1, userID);
                    statement.setBytes(2, publicKey);
                    statement.setString(3, privateKey);
                }
        );
    }

    public static PreparedStatementProvider updateKey(
            int userId,
            byte[] publicKey,
            String privateKey) throws SQLException {
        return provider("UPDATE keypairs SET privatekey = ? WHERE userid = ? AND publickey = ?",
                statement -> {
                    statement.setString(1, privateKey);
                    statement.setInt(2, userId);
                    statement.setBytes(3, publicKey);
                }
        );
    }

    public static PreparedStatementProvider deleteKey(
            int keyID) throws SQLException {
        return provider("DELETE FROM keypairs WHERE keypairid = ?",
                statement -> statement.setInt(1, keyID)
        );
    }

    public static PreparedStatementProvider deleteAllKeys(
            int userID) throws SQLException {
        return provider("DELETE FROM keypairs WHERE userid = ?",
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatementProvider getPasswordRecoveryUserID(String GUIDHash)
            throws SQLException {
        return provider("SELECT * FROM recover WHERE guidhash=? and dt BETWEEN (NOW() - INTERVAL " + RECOVERY_TIME + " SECOND) AND NOW()",
                statement -> statement.setString(1, GUIDHash)
        );
    }

    public static PreparedStatementProvider insertPasswordRecovery(int userID, String GUIDHash) throws SQLException {
        return provider("INSERT INTO recover (userid, guidhash) VALUES (?, ?)",
                statement -> {
                    statement.setInt(1, userID);
                    statement.setString(2, GUIDHash);
                }
        );
    }

    public static PreparedStatementProvider deletePasswordRecovery(String GUIDHash) throws SQLException {
        return provider("DELETE FROM recover WHERE guidhash = ?",
                statement -> statement.setString(1, GUIDHash)
        );
    }

    public static PreparedStatementProvider updateUserPassword(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        return provider("UPDATE users SET pass = ?, salt = ? WHERE id = ?",
                statement -> {
                    statement.setBytes(1, hashedPassword);
                    statement.setBytes(2, salt);
                    statement.setInt(3, userID);
                }
        );
    }

    public static PreparedStatementProvider incrementFailedLogins(int userID) throws SQLException {
        return provider("UPDATE users SET failedLogins = failedLogins + 1 WHERE id = ?",
                statement -> {
                    statement.setInt(1, userID);
                }
        );
    }

    public static PreparedStatementProvider resetFailedLogins(int userID) throws SQLException {
        return provider("UPDATE users SET failedLogins = 0 WHERE id = ?",
                statement -> {
                    statement.setInt(1, userID);
                }
        );
    }

    public static PreparedStatementProvider isFriendsWith(String username, String friend) throws SQLException {
        return provider("SELECT * FROM friends WHERE username = ? AND friend = ?",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatementProvider insertFriends(String username, String friend) throws SQLException {
        return provider("INSERT INTO friends (username, friend) VALUES (?, ?)",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatementProvider deleteFriends(String username, String friend) throws SQLException {
        return provider("DELETE FROM friends WHERE username = ? AND friend = ?",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, friend);
                }
        );
    }

    public static PreparedStatementProvider getFriends(String username) throws SQLException {
        return provider("SELECT friend FROM friends WHERE username = ?",
                statement -> statement.setString(1, username)
        );
    }

    public static PreparedStatementProvider getPeopleWhoFriendMe(String username) throws SQLException {
        return provider("SELECT username FROM friends WHERE friend = ?",
                statement -> statement.setString(1, username)
        );
    }

    public static PreparedStatementProvider insertTransaction(String fromUser,
                                                              String toUser,
                                                              long amount,
                                                              String message,
                                                              boolean isRequest) throws SQLException {
        return provider("INSERT INTO transactions (fromuser, touser, amount, message, isrequest) VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, fromUser);
                    statement.setString(2, toUser);
                    statement.setLong(3, amount);
                    statement.setString(4, message);
                    statement.setBoolean(5, isRequest);
                }
        );
    }


    public static PreparedStatementProvider updateTransactionRequestAsComplete(int tranId, String fromUser) throws SQLException {
        return provider("UPDATE transactions SET isrequest = 0 WHERE tranid = ? AND fromuser = ?",
                statement -> {
                    statement.setInt(1, tranId);
                    statement.setString(2, fromUser);
                }
        );
    }

    public static PreparedStatementProvider getTransactionRequests(String fromUser) throws SQLException {
        return provider("SELECT * FROM transactions WHERE fromuser = ? AND isrequest = ?",
                statement -> {
                    statement.setString(1, fromUser);
                    statement.setBoolean(2, true);
                }
        );
    }

    public static PreparedStatementProvider getAllTransactions(String user) throws SQLException {
        return provider("SELECT * FROM transactions WHERE fromuser = ? OR touser = ?",
                statement -> {
                    statement.setString(1, user);
                    statement.setString(2, user);
                }
        );
    }

    public static PreparedStatementProvider deleteTransactionRequest(int transId, String toUser)
            throws SQLException {
        return provider("DELETE FROM transactions WHERE tranid = ? AND fromuser = ? AND isrequest = ?",
                statement -> {
                    statement.setInt(1, transId);
                    statement.setString(2, toUser);
                    statement.setBoolean(3, true);
                }
        );
    }

    public static PreparedStatementProvider insertPendingKeyPair(int userid, byte[] publickey,
                                                                 String privatekey, String guidhash) throws SQLException {
        return provider("INSERT INTO pendingkeys (userid, publickey, privatekey, guidhash) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, userid);
                    statement.setBytes(2, publickey);
                    statement.setString(3, privatekey);
                    statement.setString(4, guidhash);

                }
        );
    }

    public static PreparedStatementProvider getPendingKeyByGuid(String guidhash) throws SQLException {
        return provider("SELECT * FROM pendingkeys WHERE guidhash = ?",
                statement -> statement.setString(1, guidhash)
        );
    }

    public static PreparedStatementProvider deletePendingKey(String guidhash) throws SQLException {
        return provider("DELETE FROM pendingkeys WHERE guidhash = ?",
                statement -> statement.setString(1, guidhash)
        );
    }

    @FunctionalInterface
    public interface PreparedStatementProvider {
        PreparedStatement get(Connection conn) throws SQLException;
    }
}
