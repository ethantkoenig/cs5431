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

    private static final String REGISTER_ERROR_ONE = "Password must be between 12 and 24 characters, contain a lowercase letter, capital letter, and a number. Username must be alphanumeric and between 6 and 24 characters.";
    private static final String REGISTER_ERROR_TWO = "Username and/or email already taken.";
    private static final String LOGIN_ERROR = "Invalid username or password.";

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
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                String email = request.queryParams("email");
                if (!ValidateUtils.validUsername(username)) {
                    return RouteUtils.modelAndView(request, "register.ftl")
                            .add("error", REGISTER_ERROR_ONE)
                            .get();
                } else if (!ValidateUtils.validPassword(password)) {
                    return RouteUtils.modelAndView(request, "register.ftl")
                            .add("error", REGISTER_ERROR_ONE)
                            .get();
                }else if (!ValidateUtils.validEmail(email)) {
                    return RouteUtils.modelAndView(request, "register.ftl")
                            .add("error", REGISTER_ERROR_ONE)
                            .get();
                } else if (UserAccess.getUserbyUsername(username).isPresent()) {
                    return RouteUtils.modelAndView(request, "register.ftl")
                            .add("error", REGISTER_ERROR_TWO)
                            .get();
                } else if (UserAccess.getUserbyEmail(email).isPresent()) {
                    return RouteUtils.modelAndView(request, "register.ftl")
                            .add("error", REGISTER_ERROR_TWO)
                            .get();
                }
                byte[] salt = Crypto.generateSalt();
                byte[] hash = Crypto.hashAndSalt(password, salt);
                UserAccess.insertUser(username, email, salt, hash);
                request.session(true).attribute("username", username);
                return RouteUtils.modelAndView(request, "register.ftl")
                        .add("success", "User registered and logged in.")
                        .get();
            }, new FreeMarkerEngine());
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

            // TODO: get wraprouter to work with freemarker
            post("", (request, response) -> {
                String username = queryParam(request, "username");
                String password = queryParam(request, "password");
                Optional<User> optUser = UserAccess.getUserbyUsername(username);
                if (!optUser.isPresent()) {
                    return RouteUtils.modelAndView(request, "login.ftl")
                            .add("error", LOGIN_ERROR)
                            .get();
                }
                User user = optUser.get();
                byte[] hash = Crypto.hashAndSalt(password, user.getSalt());
                if (!Arrays.equals(hash, user.getHashedPassword())) {
                    return RouteUtils.modelAndView(request, "login.ftl")
                            .add("error", LOGIN_ERROR)
                            .get();
                }
                request.session(true).attribute("username", username);
                return RouteUtils.modelAndView(request, "transact.ftl")
                        .get();
            }, new FreeMarkerEngine());
        });
    }

    private static void logoutUser() {
        delete("/logout", (request, response) -> {
            request.session().removeAttribute("username");
            return RouteUtils.modelAndView(request, "index.ftl")
                    .add("message", "Successfully logged out.")
                    .get();
        }, new FreeMarkerEngine());
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
            List<String> hashes = UserAccess.getKeysByUserID(user.getId()).stream()
                    .map(Key::getPublicKey)
                    .map(ByteUtil::bytesToHexString)
                    .collect(Collectors.toList());
            return RouteUtils.modelAndView(request, "user.ftl")
                    .add("username", user.getUsername())
                    .add("hashes", hashes)
                    .add("success", "Public Key added.")
                    .get();
        }));
    }
}
