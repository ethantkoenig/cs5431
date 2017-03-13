package server.controllers;

import server.dao.UserDao;
import server.models.User;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import utils.Crypto;
import utils.Pair;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

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
                    Pair<byte[], byte[]> hashedpass = Crypto.hashAndSalt(password);
                    //TODO: store salt in the database
                    userDao.insertUser(username, new String(hashedpass.getLeft(), "UTF-8"));
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
            if (nameValidator(name)) {
                user = userDao.getUserbyUsername(name);
            }
            response.type("application/json");
            if (user != null) {
                if (user.getPublicKey() != null) {
                    //TODO: hex encode but doesnt much matter here since we wont use this function anyway
                    return user.getPublicKey().getEncoded();
                } else {
                    return "null";
                }
            }
            return "{\"message\":\"User not found.\"}";
        });
    }

    private static boolean validateLength(String str, int min, int max) {
        return (str.length() > min) && (str.length() < max);
    }

    // Check for common elements of SQL statements.
    private static boolean validateNotSql(String str) {
        return !((str.contains(";")) || (str.contains("'")) || (str.contains("/*")) || (str.contains("()"))
        || str.contains("@@"));
    }

    private static boolean validateAlphanumeric(String str) {
        return str.matches("^(?=.*[a-z])(?=.*[0-9])[a-z0-9]+$");
    }

    private static boolean validateStrongAlphanumeric(String str) {
        return str.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])[A-Za-z0-9]+$");
    }

    // Name must be between 6 and 12 characters and contain both lowercase letters and numbers.
    private static boolean nameValidator(String name) {
        return validateLength(name, 6, 12) && validateAlphanumeric(name) && validateNotSql(name);
    }

    /* Password Requirements:
     * Length: 24 >= Length >= 12
     * Must contain capitals, lowercase, and numbers.
     */
    private static boolean passwordValidator(String password) {
        return validateLength(password, 12, 24) && validateStrongAlphanumeric(password) && validateNotSql(password);
    }
}
