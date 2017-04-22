package server.utils;

import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class PooledConnectionProvider implements ConnectionProvider {

    private final BasicDataSource dataSource;

    protected PooledConnectionProvider() {
        dataSource = new BasicDataSource();
        configure(dataSource);
    }

    protected abstract void configure(BasicDataSource dataSource);

    @Override
    public final Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
