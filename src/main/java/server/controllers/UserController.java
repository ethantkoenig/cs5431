package server.controllers;

import com.google.inject.Inject;
import crypto.Crypto;
import crypto.ECDSAPublicKey;
import message.IncomingMessage;
import message.Message;
import message.payloads.GetFundsRequestPayload;
import message.payloads.GetFundsResponsePayload;
import server.access.KeyAccess;
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
import utils.Optionals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;

public class UserController extends AbstractController {
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    private static final String LOCKOUT_SUBJECT = "Yaccoin account alert";
    private static final String LOCKOUT_BODY = "Your account has had several failed login attempts. For your safety, your account has been locked. Please unlock your password using the link below.";

    private static final String REGISTER_INVALID_USERNAME = "Username must be alphanumeric and between 6 and 24 characters.";
    private static final String REGISTER_INVALID_EMAIL = "Hmm, that doesn't look like an email address.";
    private static final String REGISTER_TAKEN_USERNAME_OR_EMAIL = "Username and/or email already taken.";
    private static final String LOGIN_ERROR = "Invalid username or password.";
    private static final String LOCKOUT_ALERT = "This account has been locked, check your inbox for instructions.";
    private static final int FAILED_LOGIN_LIMIT = 5;

    private final UserAccess userAccess;
    private final KeyAccess keyAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;
    private final Crypto crypto;

    @Inject
    private UserController(UserAccess userAccess,
                           KeyAccess keyAccess,
                           RouteUtils routeUtils,
                           MailService mailService,
                           Crypto crypto) {
        this.userAccess = userAccess;
        this.keyAccess = keyAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
        this.crypto = crypto;
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

        get("/user", wrapTemplate(this::viewUser), new FreeMarkerEngine());

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
        byte[] salt = crypto.generateSalt();
        byte[] hash = Crypto.hashAndSalt(password, salt);
        userAccess.insertUser(username, email, salt, hash);
        request.session(true).attribute("username", username);
        RouteUtils.successMessage(request, "Registration complete. Welcome!");
        response.redirect("/user");
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
        boolean validAuth = user.checkPassword(password);
        boolean lockedOut = user.getFailedLogins() >= FAILED_LOGIN_LIMIT;
        if (!lockedOut && !validAuth) {
            userAccess.incrementFailedLogins(user.getId());
            if (user.getFailedLogins() + 1 == FAILED_LOGIN_LIMIT) {
                String link = baseURL(request) + "/unlock";
                mailService.sendEmail(user.getEmail(), LOCKOUT_SUBJECT, lockoutBody(link));
                lockedOut = true;
            }
        }

        if (lockedOut) {
            RouteUtils.alertMessage(request, LOCKOUT_ALERT);
            response.redirect("/unlock");
            return "redirected";
        } else if (!validAuth) {
            RouteUtils.errorMessage(request, LOGIN_ERROR);
            response.redirect("/login");
            return "redirected";
        }
        userAccess.resetFailedLogins(user.getId());
        request.session(true).attribute("username", username);

        response.redirect("/user");
        return "redirected";
    }

    String logout(Request request, Response response) throws SQLException {
        request.session().removeAttribute("username");
        RouteUtils.successMessage(request, "Successfully logged out");
        return "ok"; // will be redirected to homepage via index.js
    }

    ModelAndView viewUser(Request request, Response response) throws Exception {
        User loggedInUser = routeUtils.forceLoggedInUser(request);
        String loggedInUsername = loggedInUser.getUsername();

        List<String> friends = userAccess.getFriends(loggedInUsername);
        List<String> users = userAccess.getAllUsernames();
        users.removeAll(friends);

        return routeUtils.modelAndView(request, "user.ftl")
                .add("loggedInUser", loggedInUsername)
                .add("friends", friends)
                .add("users", users)
                .get();
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
        List<Key> keys = keyAccess.getKeysByUserID(loggedInUser.getId());

        List<ECDSAPublicKey> publicKeys = keys.stream()
                .map(Key::asKey).flatMap(Optionals::stream).collect(Collectors.toList());

        GetFundsResponsePayload fundsResponse = queryForFunds(publicKeys);

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

    private GetFundsResponsePayload queryForFunds(List<ECDSAPublicKey> publicKeys) throws Exception {
        try (Socket socket = new Socket(
                Constants.getNodeAddress().getAddress(),
                Constants.getNodeAddress().getPort())) {

            new GetFundsRequestPayload(publicKeys).toMessage()
                    .serialize(new DataOutputStream(socket.getOutputStream()));

            IncomingMessage respMessage = IncomingMessage.responderlessDeserializer()
                    .deserialize(new DataInputStream(socket.getInputStream()));
            if (respMessage.type != Message.FUNDS) {
                LOGGER.severe(String.format("Unexpected response type %d, expected %d",
                        respMessage.type, Message.FUNDS));
            }
            return GetFundsResponsePayload.DESERIALIZER.deserialize(respMessage.payload);
        }
    }

    private static String lockoutBody(String link) {
        return String.format("%s%n%n%s", LOCKOUT_BODY, link);
    }

}
