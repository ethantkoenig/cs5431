package server.controllers;

import server.access.UserAccess;
import server.models.Key;
import server.models.User;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Crypto;

import java.util.*;
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
            get("", (request, response) -> {
                Map<String, Object> emptyModel = new HashMap<>();
                return new ModelAndView(emptyModel, "register.ftl");
            }, new FreeMarkerEngine());

            post("", (request, response) -> {
                response.type("application/json");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                if (!ValidateUtils.validUsername(username)) {
                    return "invalid username";
                } else if (!ValidateUtils.validPassword(password)) {
                    return "invalid password";
                } else if (UserAccess.getUserbyUsername(username) != null) {
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
            get("", (request, response) -> {
                Map<String, Object> emptyModel = new HashMap<>();
                return new ModelAndView(emptyModel, "login.ftl");
            }, new FreeMarkerEngine());

            post("", wrapRoute((request, response) -> {
                response.type("application/json");
                String username = queryParam(request, "username");
                String password = queryParam(request, "password");
                User user = UserAccess.getUserbyUsername(username);
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
            User user = UserAccess.getUserbyUsername(name);
            if (user == null) {
                // TODO 404 handling
                response.redirect("/");
                return null;
            }
            List<String> hashes = UserAccess.getKeysByUserID(user.getId()).stream()
                    .map(Key::getPublicKey)
                    .map(ByteUtil::bytesToHexString)
                    .collect(Collectors.toList());
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("hashes", hashes);
            return new ModelAndView(model, "user.ftl");
        }, new FreeMarkerEngine());
    }

    private static void addUserPublicKey() {
        post("/user/keys", wrapRoute((request, response) -> {
            byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
            byte[] privateKey = RouteUtils.queryParamHex(request, "privatekey");
            User user = RouteUtils.loggedInUser(request);
            UserAccess.insertKey(user.getId(), publicKey, privateKey);
            return "ok";
        }));
    }
}
