package server.utils;

import java.sql.Connection;

public interface ConnectionProvider {
    Connection getConnection();
}
