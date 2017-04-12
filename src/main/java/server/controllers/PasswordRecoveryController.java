package server.controllers;


import server.utils.Mail;
import server.utils.RouteUtils;
import spark.template.freemarker.FreeMarkerEngine;
import utils.Config;

import java.math.BigInteger;
import java.security.SecureRandom;

import static spark.Spark.*;
import static spark.Spark.post;
import static server.utils.RouteUtils.*;


/**
 * Created by EvanKing on 4/11/17.
 */
public class PasswordRecoveryController {

    private static Mail mail = new Mail();
    private static SecureRandom random = Config.secureRandom();

    public static void recoverPassword() {
        path("/recover", () -> {
            get("", (request, response) ->
                            RouteUtils.modelAndView(request, "recover.ftl").get()
                    , new FreeMarkerEngine());

            post("", wrapRoute((request, response) -> {
                String email = queryParam(request, "email");
                String link = request.url() + "/" + nextGUID();
                mail.sendEmail(email, link);
                return "ok";
            }));
        });
    }

    private static String nextGUID() {
        return new BigInteger(130, random).toString(32);
    }

}

