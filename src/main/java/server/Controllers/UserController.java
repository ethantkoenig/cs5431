package server.controllers;

import server.dao.UserDao;
import server.models.User;

import static spark.Spark.get;

/**
 * Created by EvanKing on 3/10/17.
 */
public class UserController {

    // Basic route controller to serve user publickey
    // This is useless, no case where we would want this but it serves as an example for you guys
    // and yall asked for it so ya...
    public static void serveUserPublicKey(UserDao userDao) {
        get("/user/:name", (request, response) -> {
            User user = null;
            String name = request.params(":name");
            if (nameValidator(name)) user = userDao.getUserbyUsername(name);
            response.type("application/json");
            if (user != null)
                if (user.getPublicKey() != null)
                    return user.getPublicKey().getEncoded();
                else
                    return "null";
            return "{\"message\":\"User not found.\"}";
        });
    }

    private static boolean nameValidator(String name){
        //TODO: validate the user input. ie length, not a sql query, etc.
        return true;
    }
}
