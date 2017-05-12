package server.access;

import server.utils.ConnectionProvider;
import server.utils.Statements;
import utils.Log;
import utils.ShaTwoFiftySix;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractAccess {
    private static final Log LOGGER = Log.forClass(AbstractAccess.class);
    private final ConnectionProvider connectionProvider;

    AbstractAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    final String hashOfGuid(String guid) {
        return ShaTwoFiftySix.hashOf(guid.getBytes(StandardCharsets.UTF_8)).toString();
    }

    final <T> T select(Statements.PreparedStatementProvider statementProvider, ResultConstructor<T> resultConstructor)
            throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement statement = statementProvider.get(conn);
             ResultSet resultSet = statement.executeQuery()) {
            return resultConstructor.get(resultSet);
        }
    }

    final void update(Statements.PreparedStatementProvider statementProvider) throws SQLException {
        updateAndReturnCount(statementProvider);
    }

    final void update(Statements.PreparedStatementProvider statementProvider, int expectedRowCount) throws SQLException {
        int actualRowCount = updateAndReturnCount(statementProvider);
        if (actualRowCount != expectedRowCount) {
            LOGGER.severe("Insert affected %d rows, expected %d",
                    actualRowCount, expectedRowCount);
        }
    }

    private int updateAndReturnCount(Statements.PreparedStatementProvider statementProvider)
            throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement statement = statementProvider.get(conn)) {
            return statement.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface ResultConstructor<T> {
        T get(ResultSet resultSet) throws SQLException;
    }

    static <T> ResultConstructor<List<T>> list(ResultConstructor<T> constructor) {
        return resultSet -> {
            List<T> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(constructor.get(resultSet));
            }
            return result;
        };
    }

    static <T> ResultConstructor<Optional<T>> optional(ResultConstructor<T> constructor) {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(constructor.get(resultSet));
        };
    }
}
