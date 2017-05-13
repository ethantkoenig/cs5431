package testutils;

import com.google.inject.Singleton;
import org.apache.commons.dbcp.BasicDataSource;
import server.utils.PooledConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
public final class TestConnectionProvider extends PooledConnectionProvider {
    private static final String DRIVER_CLASS_NAME = "org.hsqldb.jdbcDriver";
    private static final String CONNECTION_URL = "jdbc:hsqldb:mem";

    public TestConnectionProvider() throws SQLException {
        super();
        configure();
        initTables();
    }

    @Override
    protected void configure(BasicDataSource dataSource) {
        dataSource.setDriverClassName(DRIVER_CLASS_NAME);
        dataSource.setUrl(CONNECTION_URL);
        dataSource.setMinIdle(5);
    }

    private void initTables() throws SQLException {
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

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS recover ("
                    + "userid int NOT NULL,"
                    + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "guidhash varchar(2048) NOT NULL,"
                    + "FOREIGN KEY (userid)"
                    + "  REFERENCES users(id)"
                    + "  ON DELETE CASCADE"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS friends ("
                    + "username varchar(32) NOT NULL,"
                    + "friend varchar(32) NOT NULL"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                    + "tranid int NOT NULL IDENTITY,"
                    + "fromuser varchar(32) NOT NULL,"
                    + "touser varchar(32) NOT NULL,"
                    + "amount bigint NOT NULL,"
                    + "message varchar(256),"
                    + "isrequest boolean not null,"
                    + "PRIMARY KEY (tranid)"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pendingkeys ("
                    + "userid int NOT NULL,"
                    + "publickey varbinary(91) NOT NULL,"
                    + "privatekey VARCHAR(65535) NOT NULL,"
                    + "dt DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "guidhash varchar(2048) NOT NULL,"
                    + "FOREIGN KEY (userid)"
                    + "  REFERENCES users(id)"
                    + "  ON DELETE CASCADE"
                    + ")");
        }
    }
}
