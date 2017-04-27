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
    private static final String SUBJECT = "Yaccoin Password Recovery";
    private static SecureRandom random = Config.secureRandom(); // TODO use Guice


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
            post("", wrapRoute(this::postRecover));
            post("/reset", wrapRoute(this::reset));
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

    String postRecover(Request request, Response response) throws Exception {
        String email = queryParam(request, "email");
        String guid = nextGUID();
        String link = request.url() + "?guid=" + guid;

        Optional<User> user = userAccess.getUserByEmail(email);
        if (user.isPresent()) {
            passwordRecoveryAccess.insertPasswordRecovery(user.get().getId(), guid);
            mailService.sendEmail(email, SUBJECT, emailBody(link));
            RouteUtils.successMessage(request, "Check your inbox.");
        }
        response.redirect("/recover");
        return "redirected";
    }

    String reset(Request request, Response response) throws Exception {
        String password = queryParam(request, "password");
        String passwordConfirm = queryParam(request, "passwordConfirm");
        String guid = queryParam(request, "guid");
        if (!(password.equals(passwordConfirm) && ValidateUtils.validPassword(password))) {
            //error handling
            RouteUtils.errorMessage(request, PASSWORD_ERROR);
            response.redirect("/recover?guid=" + guid);
            return "redirected";
        }
        OptionalInt optUserID = passwordRecoveryAccess.getPasswordRecoveryUserID(guid);
        if (optUserID.isPresent()) {
            //update password for userid with new password.
            byte[] salt = Crypto.generateSalt();
            byte[] hash = Crypto.hashAndSalt(password, salt);
            userAccess.updateUserPass(optUserID.getAsInt(), salt, hash);
            RouteUtils.successMessage(request, "Password updated.");
            response.redirect("/login");
            return "redirected";
        }
        RouteUtils.errorMessage(request, "This link has expired. Please retry.");
        response.redirect("/recover");
        return "redirected";
    }

    private static String emailBody(String link) {
        return String.format(
                "Click on the link below to create reset your account password.%n%n%s",
                link
        );
    }

    private static String nextGUID() {
        return new BigInteger(130, random).toString(32);
    }

}

