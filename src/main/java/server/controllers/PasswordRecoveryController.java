package server.controllers;


import com.google.inject.Inject;
import crypto.Crypto;
import server.access.PasswordRecoveryAccess;
import server.access.UserAccess;
import server.models.User;
import server.utils.MailService;
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
import java.util.OptionalInt;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;


public class PasswordRecoveryController {

    private static final String PASSWORD_ERROR = "Password must be between 12 and 24 characters, contain a lowercase letter, capital letter, and a number.";

    private static SecureRandom random = Config.secureRandom();

    private final UserAccess userAccess;
    private final PasswordRecoveryAccess passwordRecoveryAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;

    @Inject
    private PasswordRecoveryController(UserAccess userAccess,
                                       PasswordRecoveryAccess passwordRecoveryAccess,
                                       RouteUtils routeUtils,
                                       MailService mailService) {
        this.userAccess = userAccess;
        this.passwordRecoveryAccess = passwordRecoveryAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
    }

    public void init() {
        path("/recover", () -> {
            get("", wrapTemplate(this::getRecover), new FreeMarkerEngine());
            post("", wrapTemplate(this::postRecover), new FreeMarkerEngine());
            post("/reset", wrapTemplate(this::reset), new FreeMarkerEngine());
        });
    }

    ModelAndView getRecover(Request request, Response response) throws Exception {
        if (!queryParamExists(request, "guid")) {
            return routeUtils.modelAndView(request, "recover.ftl").get();
        }
        String guid = queryParam(request, "guid");
        if (!passwordRecoveryAccess.getPasswordRecoveryUserID(guid).isPresent()) {
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
            passwordRecoveryAccess.insertPasswordRecovery(user.get().getId(), guid);
            mailService.sendEmail(email, link);
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
        OptionalInt optUserID = passwordRecoveryAccess.getPasswordRecoveryUserID(guid);
        if (optUserID.isPresent()) {
            //update password for userid with new password.
            byte[] salt = Crypto.generateSalt();
            byte[] hash = Crypto.hashAndSalt(password, salt);
            userAccess.updateUserPass(optUserID.getAsInt(), salt, hash);
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

