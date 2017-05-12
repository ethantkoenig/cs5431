package server.access;

import com.google.inject.Inject;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Statements;
import utils.Log;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;

public class AccountRecoveryAccess extends AbstractAccess {
    private static final Log LOGGER = Log.forClass(AccountRecoveryAccess.class);

    private final UserAccess userAccess;

    @Inject
    public AccountRecoveryAccess(ConnectionProvider connectionProvider, UserAccess userAccess) {
        super(connectionProvider);
        this.userAccess = userAccess;
    }

    /**
     * Check if guid exists in a record in the recovery table and return the associated userID
     */
    public OptionalInt getUserIdByGUID(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        return select(Statements.getPasswordRecoveryUserID(guidHash), resultSet -> {
            if (resultSet.next()) {
                return OptionalInt.of(resultSet.getInt("userid"));
            }
            return OptionalInt.empty();
        });
    }

    public Optional<User> getUserByGUID(String guid) throws SQLException {
        OptionalInt optUserID = getUserIdByGUID(guid);
        if (!optUserID.isPresent()) {
            return Optional.empty();
        }
        Optional<User> user = userAccess.getUserByID(optUserID.getAsInt());
        if (!user.isPresent()) {
            LOGGER.severe("Unknown user associated with recognized GUID");
        }
        return user;
    }

    /**
     * Add a row to the recovery table with userid, current time, and hashed guid
     */
    public void insertRecovery(int userID, String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        update(Statements.insertPasswordRecovery(userID, guidHash), 1);
    }

    public void deleteRecovery(String guid) throws SQLException {
        String guidHash = hashOfGuid(guid);
        update(Statements.deletePasswordRecovery(guidHash), 1);
    }
}
