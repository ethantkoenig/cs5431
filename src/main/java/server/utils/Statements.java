package server.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// TODO: probably doesnt belong in utils but not sure where else to put it as of yet
public class Statements {

    // Update statements
    public static final String DB_NAME = "yaccoin";
    public static final String CREATE_DB = "CREATE DATABASE yaccoin";
    public static final String USE_DB = "USE yaccoin";
    public static final String CREATE_USERS_TABLE = "CREATE TABLE users ("
            + "userid int NOT NULL AUTO_INCREMENT,"
            + "username varchar(100) NOT NULL,"
            + "pass varchar(30) NOT NULL,"
            + "PRIMARY KEY (userid)"
            + ")";
    public static final String CREATE_KEYS_TABLE = "CREATE TABLE keypairs ("
            + "keypairid int NOT NULL AUTO_INCREMENT,"
            + "userid int NOT NULL,"
            + "publickey varbinary(91) NOT NULL,"
            + "privatekey varbinary(150) NOT NULL,"
            + "PRIMARY KEY (keypairid),"
            + "INDEX userid_index (userid),"
            + "FOREIGN KEY (userid)"
            + "  REFERENCES users(userid)"
            + "  ON DELETE CASCADE"
            + ")";
    public static final String SHOW_DB_LIKE = String.format("SHOW DATABASES LIKE '%s'", DB_NAME);

    //TODO: this query will be removed. Just for testing.
    public static final String INITIAL_INSERT = "INSERT INTO users (username, pass) VALUES ('Evan','password')";


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

    public static PreparedStatement getKeysByUserID(Connection connection, int userID)
            throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "SELECT * FROM keypairs WHERE userid = ?"),
                statement -> statement.setInt(1, userID)
        );
    }

    public static PreparedStatement insertUser(Connection connection,
                                               String username,
                                               String password) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO users (username, pass) VALUES (?, ?)"),
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, password);
                }
        );
    }

    public static PreparedStatement insertKey(Connection connection,
                                              int userID,
                                              byte[] publicKey,
                                              byte[] privateKey) throws SQLException {
        return prepareStatement(connection.prepareStatement(
                "INSERT INTO keypairs (userid, publickey, privatekey) VALUES (?, ?, ?)"),
                statement -> {
                    statement.setInt(1, userID);
                    statement.setBytes(2, publicKey);
                    statement.setBytes(3, privateKey);
                }
        );

    }
}
