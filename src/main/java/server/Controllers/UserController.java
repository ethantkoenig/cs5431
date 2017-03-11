package server.controllers;

import server.dao.UserDao;
import server.models.User;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;

public class UserController {

    public static void startUserController(UserDao userDao) {
        registerUser(userDao);
        serveUserPublicKey(userDao);
    }

    /**
     * Route to server register page on get and post username and password on post.
     * On post, add user to users table.
     */
    private static void registerUser(UserDao userDao) {
        path("/register", () -> {
            get("", (request, response) -> {
                Map<String, Object> emptyModel = new HashMap<>();
                return new ModelAndView(emptyModel, "register.ftl");
            }, new FreeMarkerEngine());

            post("", (request, response) -> {
                response.type("application/json");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                if (nameValidator(username) && passwordValidator(password)) {
                    userDao.insertUser(username, password);
                    return "{\"message\":\"User registered.\"}";
                } else {
                    return "{\"message\":\"Unable to add user. Check fields and try again. \"}";
                }
            });
        });

    }

    // Basic route controller to serve user publickey
    // This is useless, no case where we would want this but it serves as an example for you guys
    // and yall asked for it so ya...
    private static void serveUserPublicKey(UserDao userDao) {
        get("/user/:name", (request, response) -> {
            User user = null;
            String name = request.params(":name");
            if (nameValidator(name)) user = userDao.getUserbyUsername(name);
            response.type("application/json");
            if (user != null)
                if (user.getPublicKey() != null)
                    //TODO: hex encode but doesnt much matter here since we wont use this function anyway
                    return user.getPublicKey().getEncoded();
                else
                    return "null";
            return "{\"message\":\"User not found.\"}";
        });
    }

    private static boolean nameValidator(String name) {
        //TODO: validate the user input. ie length, not a sql query, etc.
        return true;
    }

    private static boolean passwordValidator(String password) {
        //TODO: validate that it is a proper and strong password
        return true;
    }
}
