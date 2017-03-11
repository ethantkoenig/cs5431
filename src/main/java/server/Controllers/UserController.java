package server.controllers;

import server.dao.UserDao;
import server.models.User;

import static spark.Spark.get;

/**
 * Created by EvanKing on 3/10/17.
 */
public class UserController {

    // Basic route controller to serve user
    public static void serveUserPublicKey(UserDao userDao) {
        get("/user/:name", (request, response) -> {
            // TODO: server side validation on name properties.
            String name = request.params(":name");
            User user = userDao.getUserbyUsername(name);
            response.type("application/json");
            return user.serialize();
        });
    }
}
