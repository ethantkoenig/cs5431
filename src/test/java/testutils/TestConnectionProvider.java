package testutils;

import org.junit.Assert;
import server.utils.ConnectionProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestConnectionProvider implements ConnectionProvider {
    public static final String DRIVER_CLASS_NAME = "org.hsqldb.jdbcDriver";
    public static final String CONNECTION_URL = "jdbc:hsqldb:mem";

    @Override
    public Connection getConnection() {
        try {
            Class.forName(DRIVER_CLASS_NAME);
            return DriverManager.getConnection(CONNECTION_URL);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            Assert.fail();
            return null;
        }
    }

    public void initTables() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "id INT NOT NULL IDENTITY,"
                    + "username varchar(32) NOT NULL,"
                    + "email varchar(128) NOT NULL,"
                    + "salt varbinary(32) NOT NULL,"
                    + "pass varbinary(2048) NOT NULL,"
                    + "failedLogins int DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE (username),"
                    + "UNIQUE (email)"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS keypairs ("
                    + "keypairid int NOT NULL IDENTITY,"
                    + "userid int NOT NULL,"
                    + "publickey varbinary(91) NOT NULL,"
                    + "privatekey VARCHAR(65535) NOT NULL,"
                    + "PRIMARY KEY (keypairid),"
                    + "FOREIGN KEY (userid)"
                    + "  REFERENCES users(id)"
                    + "  ON DELETE CASCADE"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS passrecover ("
                    + "userid int NOT NULL,"
                    + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "guidhash varchar(2048) NOT NULL,"
                    + "FOREIGN KEY (userid)"
                    + "  REFERENCES users(id)"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS friends ("
                    + "username varchar(32) NOT NULL,"
                    + "friend varchar(32) NOT NULL"
                    + ")");
        }
    }
}
