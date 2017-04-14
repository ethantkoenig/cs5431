package server.controllers;


import com.google.inject.Inject;
import crypto.Crypto;
import server.access.PasswordRecoveryAccess;
import server.access.UserAccess;
import server.models.User;
import server.utils.Mail;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.Config;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;

import static server.utils.RouteUtils.queryParam;
import static server.utils.RouteUtils.wrapTemplate;
import static spark.Spark.*;


public class PasswordRecoveryController {

    private static final String PASSWORD_ERROR = "Password must be between 12 and 24 characters, contain a lowercase letter, capital letter, and a number.";

    private static SecureRandom random = Config.secureRandom();

    private final UserAccess userAccess;
    private final RouteUtils routeUtils;

    @Inject
    private PasswordRecoveryController(UserAccess userAccess, RouteUtils routeUtils) {
        this.userAccess = userAccess;
        this.routeUtils = routeUtils;
    }

    public void init() {
        path("/recover", () -> {
            get("", wrapTemplate(this::getRecover), new FreeMarkerEngine());
            post("", wrapTemplate(this::postRecover), new FreeMarkerEngine());
            post("/reset", wrapTemplate(this::reset), new FreeMarkerEngine());
        });
    }

    ModelAndView getRecover(Request request, Response response) throws Exception {
        if (!request.queryParams().contains("guid")) {
            return routeUtils.modelAndView(request, "recover.ftl").get();
        }
        String guid = queryParam(request, "guid");
        int userID = PasswordRecoveryAccess.getPasswordRecoveryUserID(guid);
        if (userID == -1) {
            return routeUtils.modelAndView(request, "recover.ftl").get();
        }
        return routeUtils.modelAndView(request, "resetpass.ftl")
                .add("guid", guid)
                .get();
    }

    ModelAndView postRecover(Request request, Response response) throws Exception {
        String email = queryParam(request, "email");
        String guid = nextGUID();
        String link = request.url() + "?guid=" + guid;

        Optional<User> user = userAccess.getUserbyEmail(email);
        if (user.isPresent()) {
            PasswordRecoveryAccess.insertPasswordRecovery(user.get().getId(), guid);
            Mail.sendEmail(email, link);
            return routeUtils.modelAndView(request, "recover.ftl")
                    .add("success", "Check your inbox.")
                    .get();
        }
        // Failed but do not want to give hacker any reason to know anything happened.
        return routeUtils.modelAndView(request, "recover.ftl").get();
    }

    ModelAndView reset(Request request, Response response) throws Exception {
        String password = queryParam(request, "password");
        String passwordConfirm = queryParam(request, "passwordConfirm");
        String guid = queryParam(request, "guid");
        if (!(password.equals(passwordConfirm) && ValidateUtils.validPassword(password))) {
            //error handling
            return routeUtils.modelAndView(request, "resetpass.ftl")
                    .add("guid", guid)
                    .add("error", PASSWORD_ERROR)
                    .get();
        }
        int userID = PasswordRecoveryAccess.getPasswordRecoveryUserID(guid);
        if (userID != -1) {
            //update password for userid with new password.
            byte[] salt = Crypto.generateSalt();
            byte[] hash = Crypto.hashAndSalt(password, salt);
            userAccess.updateUserPass(userID, salt, hash);
            return routeUtils.modelAndView(request, "resetpass.ftl")
                    .add("guid", guid)
                    .add("success", "Password updated. You may go login.")
                    .get();
        }
        return routeUtils.modelAndView(request, "resetpass.ftl")
                .add("guid", guid)
                .get();
    }

    private static String nextGUID() {
        return new BigInteger(130, random).toString(32);
    }

}

