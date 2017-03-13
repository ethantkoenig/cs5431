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
            + "publickey varbinary(100) DEFAULT NULL,"
            + "PRIMARY KEY (userid)"
            + ")";
    public static final String SHOW_DB_LIKE = String.format("SHOW DATABASES LIKE '%s'", DB_NAME);

    // Select statements
    private static final String SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?";

    // Insert statements
    private static final String INSERT_USER = "INSERT INTO users (username, pass) VALUES (?, ?)";

    //TODO: this query will be removed. Just for testing.
    public static final String INITIAL_INSERT = "INSERT INTO users (username, pass) VALUES ('Evan','password')";


    public static PreparedStatement selectUserByUsername(Connection connection, String username)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)) {
            statement.setString(1, username);
            return statement;
        }
    }

    public static PreparedStatement insertUser(Connection connection,
                                               String username,
                                               String password) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER)) {
            statement.setString(1, username);
            statement.setString(2, password);
            return statement;
        }
    }
}
