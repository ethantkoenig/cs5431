package server.access;

import server.models.Key;
import server.models.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserAccess {

    /**
     * @returns a list of all String usernames
     */
    List<String> getAllUsernames() throws SQLException;

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


    /**
     * @returns true if username is friends with friend. Thus, friend can send username money
     */
    boolean isFriendsWith(String username, String friend) throws SQLException;

    /**
     * Adds the username friend combo to friends database
     */
    void insertFriends(String username, String friend) throws SQLException;

    /**
     * Removes the username friend combo from friends database
     */
    void deleteFriends(String username, String friend) throws SQLException;

    /**
     * @returns a list of username strings to display on front end
     */
    List<String> getFriends(String username) throws SQLException;

    /**
     * @returns a list of username strings that have befriended username, thus, username can send funds to these people
     */
    List<String> getPeopleWhoFriendMe(String username) throws SQLException;
}
