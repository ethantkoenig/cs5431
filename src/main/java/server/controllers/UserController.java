package server.controllers;

import server.dao.UserDao;
import server.models.Key;
import server.models.User;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Crypto;

import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class UserController {

    public static void startUserController(UserDao userDao) {
        registerUser(userDao);
        loginUser(userDao);
        logoutUser();
        viewUser(userDao);
        addUserPublicKey(userDao);
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
                if (ValidateUtils.validUsername(username)
                        && ValidateUtils.validPassword(password)) {
                    byte[] salt = Crypto.generateSalt();
                    byte[] hash = Crypto.hashAndSalt(password, salt);
                    userDao.insertUser(username, salt, hash);  // TODO check return value
                    request.session(true).attribute("username", username);
                    return "{\"message\":\"User registered.\"}";
                } else {
                    return "{\"message\":\"Unable to add user. Check fields and try again. \"}";
                }
            });
        });
    }

    /**
     * Route to server login page on get and post username and password on post.
     */
    private static void loginUser(UserDao userDao) {
        path("/login", () -> {
            get("", (request, response) -> {
                Map<String, Object> emptyModel = new HashMap<>();
                return new ModelAndView(emptyModel, "login.ftl");
            }, new FreeMarkerEngine());

            post("", (request, response) -> {
                response.type("application/json");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                User user = userDao.getUserbyUsername(username);
                if (user == null) {
                    // TODO handle
                    return "user does not exist";
                }
                byte[] hash = Crypto.hashAndSalt(password, user.getSalt());
                if (!Arrays.equals(hash, user.getHashedPassword())) {
                    // TODO handle
                    return "wrong password!";
                }
                request.session(true).attribute("username", username);
                return "ok";
            });
        });
    }

    private static void logoutUser() {
        delete("/logout", (request, response) -> {
            request.session().removeAttribute("username");
            return "ok";
        });
    }

    private static void viewUser(UserDao userDao) {
        get("/user/:name", (request, response) -> {
            String name = request.params(":name");
            User user = userDao.getUserbyUsername(name);
            if (user == null) {
                // TODO 404 handling
                response.redirect("/");
                return null;
            }
            List<Key> keys = userDao.getKeysByUserID(user.getId());
            List<String> hashes = keys.stream().map(key ->
                    ByteUtil.bytesToHexString(key.getPublicKey())
            ).collect(Collectors.toList());
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("hashes", hashes);
            return new ModelAndView(model, "user.ftl");
        }, new FreeMarkerEngine());
    }

    private static void addUserPublicKey(UserDao userDao) {
        post("/user/:name/keys", (request, response) -> {
            String name = request.params(":name");
            String publicKeyStr = request.queryParams("publickey");
            String privateKeyStr = request.queryParams("privatekey");

            Optional<byte[]> publicKeyOpt = ByteUtil.hexStringToByteArray(publicKeyStr);
            Optional<byte[]> privateKeyOpt = ByteUtil.hexStringToByteArray(privateKeyStr);
            User user = userDao.getUserbyUsername(name);

            if (!publicKeyOpt.isPresent() || !privateKeyOpt.isPresent()) {
                // TODO 404 handling
                return "invalid keys";
            }
            if (user == null) {
                // TODO 404 handling
                return "invalid username";
            }
            String sessionUsername = request.session().attribute("username");
            if (sessionUsername == null) {
                // TODO redirect to login?
                return "must be logged in";
            }
            if (!request.session().attribute("username").equals(name)) {
                // TODO 403 handling
                return "cannot change another user's keys";
            }
            userDao.insertKey(user.getId(), publicKeyOpt.get(), privateKeyOpt.get()); // TODO check return value
            return "ok";
        });
    }
}
