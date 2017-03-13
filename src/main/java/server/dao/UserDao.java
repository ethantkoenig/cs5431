package server.dao;

import server.models.User;
import server.utils.DbUtil;
import server.utils.Statements;
import utils.Crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The User Data Access Model is the layer between user objects and the Users table in the DB. The
 * {@code UserDao} queries the database in order to return user objects. It also updates, modifies, user
 * objects in DB.
 *
 * @version 1.0, March 11 2017
 */
public class UserDao {

    private static final Logger LOGGER = Logger.getLogger(UserDao.class.getName());

    public ArrayList<User> getUsers() {
        return null;
    }


    /**
     * Given a username return the user object in the DB that is associated with this username
     * NOTE: we will need to not allow duplicate usernames.
     *
     * @param username the username of the user being queried
     * @throws SQLException
     */
    public User getUserbyUsername(String username) throws SQLException {
        try (
                Connection conn = DbUtil.getConnection(false);
                PreparedStatement preparedStmt = Statements.selectUserByUsername(conn, username);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int userid = rs.getInt("userid");
                byte[] key = rs.getBytes("publickey");
                PublicKey publicKey = (key == null) ? null
                        : Crypto.deserializePublicKey(ByteBuffer.wrap(key));
                return new User(userid, username, publicKey);
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts a user into the ssers table in the yaccoin database
     *
     * @param username
     * @param password
     * @throws SQLException
     */
    public boolean insertUser(String username, String password) throws SQLException {
        try (
                Connection conn = DbUtil.getConnection(false);
                PreparedStatement preparedStmt = Statements.insertUser(conn, username, password)
        ) {
            return preparedStmt.executeUpdate() == 1;
        }
    }
}
