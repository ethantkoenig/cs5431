package server.access;

import com.google.inject.Inject;
import server.models.Key;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class KeyAccess extends AbstractAccess {

    @Inject
    public KeyAccess(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public List<Key> getKeysByUserID(int userID) throws SQLException {
        return select(Statements.getKeysByUserID(userID), list(this::getKey));
    }

    public Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException {
        return select(Statements.getKey(userID, publicKey), optional(this::getKey));
    }

    public void updateKey(int userID, byte[] publicKey, String encryptedPrivateKey) throws SQLException {
        update(Statements.updateKey(userID, publicKey, encryptedPrivateKey), 1);
    }

    public void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException {
        update(Statements.insertKey(userID, publicKey, privateKey), 1);
    }

    public void deleteKey(int keyID) throws SQLException {
        update(Statements.deleteKey(keyID), 1);
    }

    public void deleteAllKeys(int userID) throws SQLException {
        update(Statements.deleteAllKeys(userID));
    }

    public void insertPendingKey(int userid, byte[] publickey, String privatekey, String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        update(Statements.insertPendingKeyPair(userid, publickey, privatekey, guidHash), 1);
    }

    public Optional<Key> lookupPendingKey(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        return select(Statements.getPendingKeyByGuid(guidHash), optional(resultSet -> {
            int id = resultSet.getInt("userid");
            byte[] publicKeyBytes = resultSet.getBytes("publickey");
            String encryptedPrivateKeyBytes = resultSet.getString("privatekey");
            return new Key(-1, id, publicKeyBytes, encryptedPrivateKeyBytes);
        }));
    }

    public void removePendingKey(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        update(Statements.deletePendingKey(guidHash), 1);
    }

    private Key getKey(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("keypairid");
        int userId = resultSet.getInt("userid");
        byte[] publicKeyBytes = resultSet.getBytes("publickey");
        String encryptedPrivateKeyBytes = resultSet.getString("privatekey");
        return new Key(id, userId, publicKeyBytes, encryptedPrivateKeyBytes);
    }
}
