package server.access;

import server.models.Key;
import server.models.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserAccess {

    /**
     * Given a username return the user object in the DB that is associated with this username
     * NOTE: we will need to not allow duplicate usernames.
     *
     * @param username the username of the user being queried
     */
    Optional<User> getUserbyUsername(String username) throws SQLException;

    /**
     * Given an email return the user object in the DB that is associated with this username
     *
     * @param email the email of the user being queried
     */

    Optional<User> getUserbyEmail(String email) throws SQLException;

    /**
     * @return The keys associated with a given userID
     */
    List<Key> getKeysByUserID(int userID) throws SQLException;

    Optional<Key> getKey(int userID, byte[] publicKey) throws SQLException;

    /**
     * Add the given public/private keys to the database, under the given userID.
     */
    void insertKey(int userID, byte[] publicKey, String privateKey) throws SQLException;

    /**
     * Inserts a user into the users table in the yaccoin database
     */
    void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException;

    void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException;

    /**
     * Increments the failed login attempts associated with the given userID
     */
    void incrementFailedLogins(int userID) throws SQLException;

    /**
     * Resets the failed login attempts associated with the given userID to 0
     */
    void resetFailedLogins(int userID) throws SQLException;
}
