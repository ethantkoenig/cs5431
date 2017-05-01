package server.controllers;

import com.google.inject.Inject;
import crypto.Crypto;
import crypto.ECDSAPublicKey;
import network.*;
import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.Constants;
import server.utils.MailService;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Config;
import utils.Optionals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;

public class UserController {
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    private static final String REGISTER_INVALID_USERNAME = "Username must be alphanumeric and between 6 and 24 characters.";
    private static final String REGISTER_INVALID_EMAIL = "Hmm, that doesn't look like an email address.";
    private static final String REGISTER_TAKEN_USERNAME_OR_EMAIL = "Username and/or email already taken.";
    private static final String LOGIN_ERROR = "Invalid username or password.";
    private static final String LOCKOUT_ALERT = "For your safety, your account has been locked due to too many failed login attempts. Please reset your password below.";
    private static final int FAILED_LOGIN_LIMIT = 5;

    private static SecureRandom random = Config.secureRandom(); // TODO use Guice
    private static final String SUBJECT = "Yaccoin New Key";

    private final UserAccess userAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;

    @Inject
    private UserController(UserAccess userAccess, RouteUtils routeUtils, MailService mailService) {
        this.userAccess = userAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
    }

    public void init() {
        path("/register", () -> {
            get("", routeUtils.template("register.ftl"), new FreeMarkerEngine());
            post("", wrapRoute(this::register));
        });

        path("/login", () -> {
            get("", routeUtils.template("login.ftl"), new FreeMarkerEngine());
            post("", wrapRoute(this::login));
        });

        delete("/logout", wrapRoute(this::logout));

        path("/user", () -> {
            get("/:name", wrapTemplate(this::viewUser), new FreeMarkerEngine());
            post("/keys", wrapRoute(this::addUserKey));
            delete("/keys", wrapRoute(this::deleteKey));
            get("/keys/addkey", wrapRoute(this::finalizeInsertKey));

        });

        get("/balance", wrapTemplate(this::balance), new FreeMarkerEngine());

        path("/friend", () -> {
            post("", wrapRoute(this::addFriend));
            delete("", wrapRoute(this::deleteFriend));
        });
    }

    String register(Request request, Response response)
            throws Exception {
        String username = queryParam(request, "username");
        String password = queryParam(request, "password");
        String email = queryParam(request, "email");
        if (!ValidateUtils.validUsername(username)) {
            RouteUtils.errorMessage(request, REGISTER_INVALID_USERNAME);
            response.redirect("/register");
            return "redirected";
        } else if (!ValidateUtils.validPassword(password)) {
            throw new InvalidParamException("Invalid password");
        } else if (!ValidateUtils.validEmail(email)) {
            RouteUtils.errorMessage(request, REGISTER_INVALID_EMAIL);
            response.redirect("/register");
            return "redirected";
        } else if (userAccess.getUserByUsername(username).isPresent()) {
            RouteUtils.errorMessage(request, REGISTER_TAKEN_USERNAME_OR_EMAIL);
            response.redirect("/register");
            return "redirected";
        } else if (userAccess.getUserByEmail(email).isPresent()) {
            RouteUtils.errorMessage(request, REGISTER_TAKEN_USERNAME_OR_EMAIL);
            response.redirect("/register");
            return "redirected";
        }
        byte[] salt = Crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(password, salt);
        userAccess.insertUser(username, email, salt, hash);
        request.session(true).attribute("username", username);
        RouteUtils.successMessage(request, "Registration complete. Welcome!");
        response.redirect("/user/" + username);
        return "ok";
    }

    String login(Request request, Response response) throws Exception {
        String username = queryParam(request, "username");
        String password = queryParam(request, "password");
        Optional<User> optUser = userAccess.getUserByUsername(username);
        if (!optUser.isPresent()) {
            RouteUtils.errorMessage(request, LOGIN_ERROR);
            response.redirect("/login");
            return "redirected";
        }
        User user = optUser.get();
        if (user.getFailedLogins() >= FAILED_LOGIN_LIMIT) {
            RouteUtils.alertMessage(request, LOCKOUT_ALERT);
            response.redirect("/recover");
            return "redirected";
        }
        if (!validPassword(user, password)) {
            userAccess.incrementFailedLogins(user.getId());
            RouteUtils.errorMessage(request, LOGIN_ERROR);
            response.redirect("/login");
            return "redirected";
        }
        userAccess.resetFailedLogins(user.getId());
        request.session(true).attribute("username", username);

        response.redirect("/user/" + username);
        return "redirected";
    }

    String logout(Request request, Response response) throws SQLException {
        request.session().removeAttribute("username");
        RouteUtils.successMessage(request, "Successfully logged out");
        return "ok"; // will be redirected to homepage via index.js
    }

    ModelAndView viewUser(Request request, Response response) throws Exception {
        String name = request.params(":name");
        Optional<User> optUser = userAccess.getUserByUsername(name);
        if (!optUser.isPresent()) {
            // TODO 404 handling
            return RouteUtils.redirectTo(response, "/login");
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

        return routeUtils.modelAndView(request, "user.ftl")
                .add("username", user.getUsername())
                .add("loggedInUser", loggedInUserName)
                .add("friends", friends)
                .add("users", users)
                .get();
    }

    String finalizeInsertKey(Request request, Response response) throws Exception {
        String guid = RouteUtils.queryParam(request, "guid");

        User user = routeUtils.forceLoggedInUser(request);

        Optional<Key> key = userAccess.flushPendingKey(guid);
        if (!key.isPresent()) return "Did not find GUID";
        userAccess.removePendingKey(guid);
        userAccess.insertKey(key.get().getUserId(), key.get().getPublicKey(), key.get().encryptedPrivateKey);
        response.redirect("/user/" + user.getUsername());
        return "ok";
    }

    String addUserKey(Request request, Response response) throws Exception {
        byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
        String privateKey = RouteUtils.queryParam(request, "privatekey");
        String password = RouteUtils.queryParam(request, "password");
        String guid = nextGUID();
        String link = request.url() + "/addkey" + "?guid=" + guid;

        User user = routeUtils.forceLoggedInUser(request);

        if (!validPassword(user, password)) {
            // user mistyped password
            RouteUtils.errorMessage(request, "Incorrect password");
            response.redirect("/user/" + user.getUsername());
            return "redirected";
        }

        String email = user.getEmail();
        mailService.sendEmail(email, SUBJECT, emailBody(link));
        RouteUtils.successMessage(request, "Check your inbox.");

        userAccess.insertPendingKey(user.getId(), publicKey, privateKey.getBytes("UTF-8"), guid);
        response.redirect("/user/" + user.getUsername());
        return "redirected";

    }

    String deleteKey(Request request, Response response) throws Exception {
        byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
        User user = routeUtils.forceLoggedInUser(request);
        Optional<Key> optKey = userAccess.getKey(user.getId(), publicKey);
        if (optKey.isPresent()) {
            userAccess.deleteKey(optKey.get().getId());
        }
        return "ok";
    }

    String addFriend(Request request, Response response) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String friend = RouteUtils.queryParam(request, "friend");
        String username = loggedInUser.getUsername();

        userAccess.insertFriends(username, friend);
        return "ok";
    }

    String deleteFriend(Request request, Response response) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String friend = RouteUtils.queryParam(request, "friend");
        String username = loggedInUser.getUsername();

        userAccess.deleteFriends(username, friend);
        return "ok";
    }

    ModelAndView balance(Request request, Response response) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        List<Key> keys = userAccess.getKeysByUserID(loggedInUser.getId());

        List<ECDSAPublicKey> publicKeys = keys.stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());

        GetFundsResponse fundsResponse;
        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {

            GetFundsRequest fundsRequest = new GetFundsRequest(publicKeys);
            byte[] payload = ByteUtil.asByteArray(fundsRequest::serialize);
            new OutgoingMessage(Message.GET_FUNDS, payload)
                    .serialize(new DataOutputStream(socket.getOutputStream()));

            IncomingMessage respMessage = IncomingMessage.responderlessDeserializer()
                    .deserialize(new DataInputStream(socket.getInputStream()));
            if (respMessage.type != Message.FUNDS) {
                LOGGER.severe(String.format("Unexpected response type %d, expected %d",
                        respMessage.type, Message.FUNDS));
                RouteUtils.errorMessage(request, "An internal error has occurred.");
                return RouteUtils.redirectTo(response, "/");
            }
            fundsResponse = GetFundsResponse.DESERIALIZER.deserialize(respMessage.payload);
        }
        long totalBalance = fundsResponse.keyFunds.values().stream()
                .mapToLong(Long::longValue).sum();
        Map<String, Long> balancesByKey = fundsResponse.keyFunds.entrySet().stream()
                .collect(Collectors.toMap(entry -> ByteUtil.bytesToHexString(
                        ByteUtil.forceByteArray(entry.getKey()::serialize)
                ), Map.Entry::getValue));
        return routeUtils.modelAndView(request, "balance.ftl")
                .add("balances", balancesByKey)
                .add("total", totalBalance)
                .get();
    }

    // XXX: Don't duplicate these.
    private static String emailBody(String link) {
        return String.format(
                "Click on the link below to verify new key.%n%n%s",
                link
        );
    }

    private static String nextGUID() {
        return new BigInteger(130, random).toString(32);
    }

    private boolean validPassword(User user, String password) throws Exception {
        byte[] hash = Crypto.hashAndSalt(password, user.getSalt());
        return Arrays.equals(hash, user.getHashedPassword());
    }
}
