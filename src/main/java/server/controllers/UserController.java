package server.controllers;

import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import crypto.Crypto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.queryParam;
import static server.utils.RouteUtils.wrapRoute;
import static spark.Spark.*;

public class UserController {

    public static void startUserController() {
        registerUser();
        loginUser();
        logoutUser();
        viewUser();
        addUserPublicKey();
    }

    /**
     * Route to server register page on get and post username and password on post.
     * On post, add user to users table.
     */
    private static void registerUser() {
        path("/register", () -> {
            get("", (request, response) ->
                            RouteUtils.modelAndView(request, "register.ftl").get()
                    , new FreeMarkerEngine());

            post("", (request, response) -> {
                response.type("application/json");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                if (!ValidateUtils.validUsername(username)) {
                    return "invalid username";
                } else if (!ValidateUtils.validPassword(password)) {
                    return "invalid password";
                } else if (UserAccess.getUserbyUsername(username).isPresent()) {
                    return "username already taken";
                }
                byte[] salt = Crypto.generateSalt();
                byte[] hash = Crypto.hashAndSalt(password, salt);
                UserAccess.insertUser(username, salt, hash);
                request.session(true).attribute("username", username);
                return "{\"message\":\"User registered.\"}";
            });
        });
    }

    /**
     * Route to server login page on get and post username and password on post.
     */
    private static void loginUser() {
        path("/login", () -> {
            get("", (request, response) ->
                            RouteUtils.modelAndView(request, "login.ftl").get()
                    , new FreeMarkerEngine());

            post("", wrapRoute((request, response) -> {
                response.type("application/json");
                String username = queryParam(request, "username");
                String password = queryParam(request, "password");
                Optional<User> optUser = UserAccess.getUserbyUsername(username);
                if (!optUser.isPresent()) {
                    // TODO handle
                    return "user does not exist";
                }
                User user = optUser.get();
                byte[] hash = Crypto.hashAndSalt(password, user.getSalt());
                if (!Arrays.equals(hash, user.getHashedPassword())) {
                    // TODO handle
                    return "wrong password!";
                }
                request.session(true).attribute("username", username);
                return "ok";
            }));
        });
    }

    private static void logoutUser() {
        delete("/logout", (request, response) -> {
            request.session().removeAttribute("username");
            return "ok";
        });
    }

    private static void viewUser() {
        get("/user/:name", (request, response) -> {
            String name = request.params(":name");
            Optional<User> optUser = UserAccess.getUserbyUsername(name);
            if (!optUser.isPresent()) {
                // TODO 404 handling
                response.redirect("/");
                return null;
            }
            User user = optUser.get();
            List<String> hashes = UserAccess.getKeysByUserID(user.getId()).stream()
                    .map(Key::getPublicKey)
                    .map(ByteUtil::bytesToHexString)
                    .collect(Collectors.toList());
            return RouteUtils.modelAndView(request, "user.ftl")
                    .add("username", user.getUsername())
                    .add("hashes", hashes)
                    .get();
        }, new FreeMarkerEngine());
    }

    private static void addUserPublicKey() {
        post("/user/keys", wrapRoute((request, response) -> {
            byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
            String privateKey = RouteUtils.queryParam(request, "privatekey");
            User user = RouteUtils.forceLoggedInUser(request);
            UserAccess.insertKey(user.getId(), publicKey, privateKey);
            return "ok";
        }));
    }
}
