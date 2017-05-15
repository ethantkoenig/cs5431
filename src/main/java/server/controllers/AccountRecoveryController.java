package server.controllers;


import com.google.inject.Inject;
import crypto.Crypto;
import server.access.AccountRecoveryAccess;
import server.access.KeyAccess;
import server.access.UserAccess;
import server.bodies.KeyBody;
import server.bodies.KeysBody;
import server.models.User;
import server.utils.MailService;
import server.utils.RouteUtils;
import server.utils.RouteWrapper;
import server.utils.ValidateUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;
import utils.ByteUtil;
import utils.Log;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.OptionalInt;

import static server.utils.RouteUtils.*;
import static spark.Spark.*;
import static utils.Optionals.ifPresent;


public class AccountRecoveryController extends AbstractController {
    private static final Log LOGGER = Log.forClass(AccountRecoveryController.class);

    private static final String RECOVERY_SUBJECT = "EzraCoinL Wallet Password Recovery";
    private static final String UNLOCK_SUBJECT = "EzraCoinL Wallet Account Unlock";
    private static final String CHANGE_PASSWORD_SUBJECT = "EzraCoinL Password Change";

    private final UserAccess userAccess;
    private final KeyAccess keyAccess;
    private final AccountRecoveryAccess accountRecoveryAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;
    private final Crypto crypto;

    @Inject
    private AccountRecoveryController(UserAccess userAccess,
                                      KeyAccess keyAccess,
                                      AccountRecoveryAccess accountRecoveryAccess,
                                      RouteUtils routeUtils,
                                      MailService mailService,
                                      Crypto crypto) {
        this.userAccess = userAccess;
        this.keyAccess = keyAccess;
        this.accountRecoveryAccess = accountRecoveryAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
        this.crypto = crypto;
    }

    public void init() {
        RouteWrapper wrapper = new RouteWrapper(LOGGER);
        path("/reset", () -> {
            get("", wrapper.template(this::getReset), new FreeMarkerEngine());
            post("/mail", wrapper.route(this::resetMail));
            post("", wrapper.route(this::reset));
        });
        path("/unlock", () -> {
            get("", wrapper.template(this::getUnlock), new FreeMarkerEngine());
            post("/mail", wrapper.route(this::unlockMail));
            post("", wrapper.route(this::unlock));
        });
        path("/change_password", () -> {
            get("", wrapper.template(this::getChangePassword), new FreeMarkerEngine());
            post("/mail", wrapper.route(this::changePasswordMail));
            post("", wrapper.route(this::changePassword));
        });
    }

    ModelAndView getReset(Request request, Response response, Log log) throws Exception {
        return getRecoveryPage(request, "resetRequest.ftl", "reset.ftl");
    }

    String resetMail(Request request, Response response, Log log) throws Exception {
        String email = queryParam(request, "email");
        ifPresent(createGUID(email), guid -> {
            String link = baseURL(request) + "/reset?guid=" + guid;
            mailService.sendEmail(email, RECOVERY_SUBJECT, resetEmailBody(link));
            log.info("Password reset link sent; address=%s", email);
        });
        // Send success message regardless to prevent email guessing
        RouteUtils.successMessage(request, "Check your inbox.");
        response.redirect("/reset");
        return "redirected";
    }

    ModelAndView getUnlock(Request request, Response response, Log log) throws Exception {
        return getRecoveryPage(request, "unlockRequest.ftl", "unlock.ftl");
    }

    String unlockMail(Request request, Response response, Log log) throws Exception {
        String email = queryParam(request, "email");
        ifPresent(createGUID(email), guid -> {
            String link = baseURL(request) + "/unlock?guid=" + guid;
            mailService.sendEmail(email, UNLOCK_SUBJECT, unlockEmailBody(link));
            RouteUtils.successMessage(request, "Check your inbox.");
        });
        response.redirect("/unlock");
        return "redirected";
    }

    String unlock(Request request, Response response, Log log) throws Exception {
        String password = queryParam(request, "password");
        String guid = queryParam(request, "guid");

        Optional<User> optUser = accountRecoveryAccess.getUserByGUID(guid);
        if (!optUser.isPresent()) {
            RouteUtils.errorMessage(request, "This link has expired. Please retry.");
            response.redirect("/unlock");
            return "redirected";
        }

        User user = optUser.get();
        if (!user.checkPassword(password)) {
            accountRecoveryAccess.deleteRecovery(guid);
            RouteUtils.errorMessage(request, "Incorrect password. You will need to request a new link to unlock your account.");
            response.redirect("/unlock");
            return "redirected";
        }
        userAccess.resetFailedLogins(user.getId());
        request.session().attribute("username", user.getUsername());
        response.redirect("/user");
        log.info("Account unlocked; user=%d", user.getId());
        RouteUtils.successMessage(request, "Account unlocked");
        return "redirected";
    }

    String reset(Request request, Response response, Log log) throws Exception {
        String password = queryParam(request, "password");
        String guid = queryParam(request, "guid");

        if (!ValidateUtils.validPassword(password)) {
            throw new InvalidParamException("Invalid password");
        }
        OptionalInt optUserID = accountRecoveryAccess.getUserIdByGUID(guid);
        if (!optUserID.isPresent()) {
            RouteUtils.errorMessage(request, "This link has expired. Please retry.");
            response.redirect("/reset");
            return "redirected";
        }
        int userID = optUserID.getAsInt();
        keyAccess.deleteAllKeys(userID);
        updatePassword(userID, password);

        RouteUtils.successMessage(request, "Password updated.");
        response.redirect("/login");
        return "redirected";
    }

    ModelAndView getChangePassword(Request request, Response response, Log log) throws Exception {
        routeUtils.forceLoggedInUser(request);
        return getRecoveryPage(request, "changePasswordRequest.ftl", "changePassword.ftl");
    }

    String changePasswordMail(Request request, Response response, Log log) throws Exception {
        User user = routeUtils.forceLoggedInUser(request);
        String guid = crypto.nextGUID();
        accountRecoveryAccess.insertRecovery(user.getId(), guid);
        String link = baseURL(request) + "/change_password?guid=" + guid;
        mailService.sendEmail(user.getEmail(), CHANGE_PASSWORD_SUBJECT, changePasswordEmailBody(link));
        RouteUtils.successMessage(request, "Check your inbox.");
        response.redirect("/change_password");
        return "redirected";
    }

    String changePassword(Request request, Response response, Log log) throws Exception {
        String guid = queryParam(request, "guid");
        String password = queryParam(request, "password");

        User user = accountRecoveryAccess.getUserByGUID(guid).orElseThrow(() -> {
            RouteUtils.errorMessage(request, "This link has expired. Please retry.");
            return new RouteUtils.InvalidParamException("Invalid guid");
        });

        KeysBody keys = routeUtils.parseBody(request, KeysBody.class);
        for (KeyBody key : keys.keys) {
            byte[] publicKey = ByteUtil.hexStringToByteArray(key.publicKey)
                    .orElseThrow(() -> new InvalidParameterException("Invalid public key"));
            keyAccess.updateKey(user.getId(), publicKey, key.encryptedPrivateKey);
        }

        updatePassword(user.getId(), password);
        RouteUtils.successMessage(request, "Successfully changed password.");
        return "ok";
    }

    private ModelAndView getRecoveryPage(Request request, String requestView, String formView)
            throws Exception {
        if (!queryParamExists(request, "guid")) {
            return routeUtils.modelAndView(request, requestView).get();
        }
        String guid = queryParam(request, "guid");
        if (!accountRecoveryAccess.getUserIdByGUID(guid).isPresent()) {
            return routeUtils.modelAndView(request, requestView).get();
        }
        return routeUtils.modelAndView(request, formView)
                .add("guid", guid)
                .get();
    }

    private void updatePassword(int userID, String newPassword) throws Exception {
        byte[] salt = crypto.generateSalt();
        byte[] hashedPassword = Crypto.hashAndSalt(newPassword, salt);
        userAccess.updateUserPass(userID, salt, hashedPassword);
    }

    private Optional<String> createGUID(String emailAddress) throws Exception {
        String guid = crypto.nextGUID();
        Optional<User> optUser = userAccess.getUserByEmail(emailAddress);
        if (!optUser.isPresent()) {
            return Optional.empty();
        }
        User user = optUser.get();
        accountRecoveryAccess.insertRecovery(user.getId(), guid);
        return Optional.of(guid);
    }

    private static String resetEmailBody(String link) {
        return String.format(
                "Click on the link below to reset your account password.%n%n%s",
                link
        );
    }

    private static String unlockEmailBody(String link) {
        return String.format(
                "Click on the link below to create unlock your password.%n%n%s",
                link
        );
    }

    private static String changePasswordEmailBody(String link) {
        return String.format(
                "Click on the link below to change your password.%n%n%s",
                link
        );
    }
}
