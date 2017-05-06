package server.access;

import com.google.inject.Inject;
import server.models.Key;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KeyAccess extends AbstractAccess {
    private final ConnectionProvider connectionProvider;

    @Inject
    public KeyAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<Key> getKeysByUserID(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getKeysByUserID(conn, userID);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Key> keys = new ArrayList<>();
            while (rs.next()) {
                keys.add(getKey(rs));
            }
            return keys;
        }
    }

    public Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getKey(conn, userID, publicKey);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return Optional.of(getKey(rs));
            }
            return Optional.empty();
        }
    }

    public void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertKey(conn, userID, publicKey, privateKey)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    public void deleteKey(int keyID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteKey(conn, keyID)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    public void deleteAllKeys(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteAllKeys(conn, userID)) {
            preparedStmt.executeUpdate();
        }
    }

    private Key getKey(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("keypairid");
        int userId = resultSet.getInt("userid");
        byte[] publicKeyBytes = resultSet.getBytes("publickey");
        String encryptedPrivateKeyBytes = resultSet.getString("privatekey");
        return new Key(id, userId, publicKeyBytes, encryptedPrivateKeyBytes);
    }

    public void insertPendingKey(int userid, byte[] publickey, String privatekey, String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertPendingKeyPair(conn, userid, publickey, privatekey, guidHash)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    public Optional<Key> lookupPendingKey(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPendingKeyByGuid(conn, guidHash);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (!rs.next()) return Optional.empty();
            int id = rs.getInt("userid");
            byte[] publicKeyBytes = rs.getBytes("publickey");
            String encryptedPrivateKeyBytes = rs.getString("privatekey");
            return Optional.of(new Key(-1, id, publicKeyBytes, encryptedPrivateKeyBytes));
        }

    }

    public void removePendingKey(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deletePendingKey(conn, guidHash)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }
}
