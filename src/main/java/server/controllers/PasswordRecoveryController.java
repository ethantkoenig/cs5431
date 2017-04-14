package server.controllers;


import crypto.Crypto;
import server.access.PasswordRecoveryAccess;
import server.access.UserAccess;
import server.models.User;
import server.utils.Mail;
import server.utils.RouteUtils;
import server.utils.ValidateUtils;
import spark.template.freemarker.FreeMarkerEngine;
import utils.Config;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;

import static server.utils.RouteUtils.queryParam;
import static spark.Spark.*;


public class PasswordRecoveryController {

    private static final String PASSWORD_ERROR = "Password must be between 12 and 24 characters, contain a lowercase letter, capital letter, and a number.";

    private static SecureRandom random = Config.secureRandom();

    public static void recoverPassword() {
        path("/recover", () -> {

            post("", (request, response) -> {
                String email = queryParam(request, "email");
                String guid = nextGUID();
                String link = request.url() + "?guid=" + guid;
                Optional<User> user = UserAccess.getUserbyEmail(email);
                if (user.isPresent()){
                    PasswordRecoveryAccess.insertPasswordRecovery(user.get().getId(), guid);
                    Mail.sendEmail(email, link);
                    return RouteUtils.modelAndView(request, "recover.ftl")
                            .add("success", "Check your inbox.")
                            .get();
                }
                // Failed but do not want to give hacker any reason to know anything happened.
                return RouteUtils.modelAndView(request, "recover.ftl").get();
            }, new FreeMarkerEngine());

            get("", (request, response) -> {
                if (request.queryParams().contains("guid")){
                    String guid = queryParam(request, "guid");
                    int userID = PasswordRecoveryAccess.getPasswordRecoveryUserID(guid);
                    if (userID != -1) {
                        return RouteUtils.modelAndView(request, "resetpass.ftl")
                                .add("guid", guid)
                                .get();
                    }
                }
                return RouteUtils.modelAndView(request, "recover.ftl").get();
            }, new FreeMarkerEngine());

            post("/reset", (request, response) -> {
                String password = queryParam(request, "password");
                String passwordConfirm = queryParam(request, "passwordConfirm");
                String guid = queryParam(request, "guid");
                if (!(password.equals(passwordConfirm) && ValidateUtils.validPassword(password))) {
                    //error handling
                    return RouteUtils.modelAndView(request, "resetpass.ftl")
                            .add("guid", guid)
                            .add("error", PASSWORD_ERROR)
                            .get();
                }
                int userID = PasswordRecoveryAccess.getPasswordRecoveryUserID(guid);
                if (userID != -1) {
                    //update password for userid with new password.
                    byte[] salt = Crypto.generateSalt();
                    byte[] hash = Crypto.hashAndSalt(password, salt);
                    UserAccess.updateUserPass(userID, salt, hash);
                    return RouteUtils.modelAndView(request, "resetpass.ftl")
                            .add("guid", guid)
                            .add("success", "Password updated. You may go login.")
                            .get();
                }
                return RouteUtils.modelAndView(request, "resetpass.ftl")
                        .add("guid", guid)
                        .get();
            }, new FreeMarkerEngine());
        });
    }

    private static String nextGUID() {
        return new BigInteger(130, random).toString(32);
    }

}

