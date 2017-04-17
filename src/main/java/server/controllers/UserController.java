package server.controllers;

import com.google.inject.Inject;
import crypto.Crypto;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;

public class UserController {

    private static final String REGISTER_ERROR_ONE = "Password must be between 12 and 24 characters, contain a lowercase letter, capital letter, and a number. Username must be alphanumeric and between 6 and 24 characters.";
    private static final String REGISTER_ERROR_TWO = "Username and/or email already taken.";
    private static final String LOGIN_ERROR = "Invalid username or password.";
    private static final String LOCKOUT_ALERT = "For your safety, your account has been locked due to too many failed login attempts. Please reset your password below.";
    private static final int FAILED_LOGIN_LIMIT = 5;


    private final UserAccess userAccess;
    private final RouteUtils routeUtils;

    @Inject
    private UserController(UserAccess userAccess, RouteUtils routeUtils) {
        this.userAccess = userAccess;
        this.routeUtils = routeUtils;
    }

    public void init() {
        path("/register", () -> {
            get("", routeUtils.template("register.ftl"), new FreeMarkerEngine());
            post("", wrapTemplate(this::register), new FreeMarkerEngine());
        });

        path("/login", () -> {
            get("", routeUtils.template("login.ftl"), new FreeMarkerEngine());
            post("", wrapTemplate(this::login), new FreeMarkerEngine());
        });

        delete("/logout", wrapTemplate(this::logout), new FreeMarkerEngine());

        path("/user", () -> {
            get("/:name", wrapTemplate(this::viewUser), new FreeMarkerEngine());
            post("/keys", wrapTemplate(this::addUserKey), new FreeMarkerEngine());

        });

        path("/friend", () -> {
            post("", wrapRoute(this::addFriend));
            delete("", wrapRoute(this::deleteFriend));
        });
    }

    ModelAndView register(Request request, Response response)
            throws Exception {
        String username = request.queryParams("username");
        String password = request.queryParams("password");
        String email = request.queryParams("email");
        if (!ValidateUtils.validUsername(username)) {
            return routeUtils.modelAndView(request, "register.ftl")
                    .add("error", REGISTER_ERROR_ONE)
                    .get();
        } else if (!ValidateUtils.validPassword(password)) {
            return routeUtils.modelAndView(request, "register.ftl")
                    .add("error", REGISTER_ERROR_ONE)
                    .get();
        } else if (!ValidateUtils.validEmail(email)) {
            return routeUtils.modelAndView(request, "register.ftl")
                    .add("error", REGISTER_ERROR_ONE)
                    .get();
        } else if (userAccess.getUserbyUsername(username).isPresent()) {
            return routeUtils.modelAndView(request, "register.ftl")
                    .add("error", REGISTER_ERROR_TWO)
                    .get();
        } else if (userAccess.getUserbyEmail(email).isPresent()) {
            return routeUtils.modelAndView(request, "register.ftl")
                    .add("error", REGISTER_ERROR_TWO)
                    .get();
        }
        byte[] salt = Crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(password, salt);
        userAccess.insertUser(username, email, salt, hash);
        request.session(true).attribute("username", username);
        return routeUtils.modelAndView(request, "register.ftl")
                .add("success", "User registered and logged in.")
                .get();
    }

    ModelAndView login(Request request, Response response) throws Exception {
        String username = queryParam(request, "username");
        String password = queryParam(request, "password");
        Optional<User> optUser = userAccess.getUserbyUsername(username);
        if (!optUser.isPresent()) {
            return routeUtils.modelAndView(request, "login.ftl")
                    .add("error", LOGIN_ERROR)
                    .get();
        }
        User user = optUser.get();
        if (user.getFailedLogins() >= FAILED_LOGIN_LIMIT) {
            return routeUtils.modelAndView(request, "recover.ftl")
                    .add("alert", LOCKOUT_ALERT)
                    .get();
        }
        byte[] hash = Crypto.hashAndSalt(password, user.getSalt());
        if (!Arrays.equals(hash, user.getHashedPassword())) {
            userAccess.incrementFailedLogins(user.getId());
            return routeUtils.modelAndView(request, "login.ftl")
                    .add("error", LOGIN_ERROR)
                    .get();
        }
        userAccess.resetFailedLogins(user.getId());
        request.session(true).attribute("username", username);

        List<String> hashes = userAccess.getKeysByUserID(user.getId()).stream()
                .map(Key::getPublicKey)
                .map(ByteUtil::bytesToHexString)
                .collect(Collectors.toList());

        List<String> friends = userAccess.getFriends(user.getUsername());
        List<String> users = userAccess.getAllUsernames();
        users.removeAll(friends);

        return routeUtils.modelAndView(request, "user.ftl")
                .add("username", user.getUsername())
                .add("loggedInUser", username)
                .add("hashes", hashes)
                .add("friends", friends)
                .add("users", users)
                .get();
    }

    ModelAndView logout(Request request, Response response) throws SQLException {
        request.session().removeAttribute("username");
        return routeUtils.modelAndView(request, "index.ftl")
                .add("message", "Successfully logged out.")
                .get();
    }

    ModelAndView viewUser(Request request, Response response) throws Exception {
        String name = request.params(":name");
        Optional<User> optUser = userAccess.getUserbyUsername(name);
        if (!optUser.isPresent()) {
            // TODO 404 handling
            response.redirect("/");
            return null;
        }
        String loggedInUserName = null;
        List<String> friends = null;
        List<String> users = null;

        Optional<User> loggedInUser = routeUtils.loggedInUser(request);
        User user = optUser.get();

        if (loggedInUser.isPresent()) {
            loggedInUserName = loggedInUser.get().getUsername();
            friends = userAccess.getFriends(user.getUsername());
            users = userAccess.getAllUsernames();
            users.removeAll(friends);
        }

        List<String> hashes = userAccess.getKeysByUserID(user.getId()).stream()
                .map(Key::getPublicKey)
                .map(ByteUtil::bytesToHexString)
                .collect(Collectors.toList());

        return routeUtils.modelAndView(request, "user.ftl")
                .add("username", user.getUsername())
                .add("loggedInUser", loggedInUserName)
                .add("hashes", hashes)
                .add("friends", friends)
                .add("users", users)
                .get();
    }

    ModelAndView addUserKey(Request request, Response response) throws Exception {
        byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
        String privateKey = RouteUtils.queryParam(request, "privatekey");
        User user = routeUtils.forceLoggedInUser(request);
        userAccess.insertKey(user.getId(), publicKey, privateKey);
        List<String> hashes = userAccess.getKeysByUserID(user.getId()).stream()
                .map(Key::getPublicKey)
                .map(ByteUtil::bytesToHexString)
                .collect(Collectors.toList());
        return routeUtils.modelAndView(request, "user.ftl")
                .add("username", user.getUsername())
                .add("hashes", hashes)
                .add("success", "Public Key added.")
                .get();
    }

    String addFriend(Request request, Response response) throws Exception {
        routeUtils.forceLoggedInUser(request);
        Optional<User> loggedInUser = routeUtils.loggedInUser(request);
        String friend = RouteUtils.queryParam(request, "friend");
        String username = loggedInUser.get().getUsername();

        userAccess.insertFriends(username, friend);
        return "ok";
    }

    String deleteFriend(Request request, Response response) throws Exception {
        routeUtils.forceLoggedInUser(request);
        Optional<User> loggedInUser = routeUtils.loggedInUser(request);
        String friend = RouteUtils.queryParam(request, "friend");
        String username = loggedInUser.get().getUsername();

        userAccess.deleteFriends(username, friend);
        return "ok";
    }
}
