package server.controllers;

import com.google.inject.Inject;
import crypto.ECDSAPublicKey;
import server.access.KeyAccess;
import server.models.Key;
import server.models.User;
import server.utils.MailService;
import server.utils.RouteUtils;
import spark.Request;
import spark.Response;
import utils.Config;
import utils.DeserializationException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

import static server.utils.RouteUtils.wrapRoute;
import static spark.Spark.*;

public class KeyController extends AbstractController {
    private static final String SUBJECT = "Yaccoin New Key";
    private static SecureRandom random = Config.secureRandom(); // TODO use Guice

    private final KeyAccess keyAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;

    @Inject
    public KeyController(KeyAccess keyAccess,
                         RouteUtils routeUtils,
                         MailService mailService) {
        this.keyAccess = keyAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
    }

    public void init() {
        path("/keys", () -> {
            post("", wrapRoute(this::addUserKey));
            delete("", wrapRoute(this::deleteKey));
            get("addkey", wrapRoute(this::finalizeInsertKey));
        });
    }

    // TODO: Get request modifies state. Should fix this somehow.
    String finalizeInsertKey(Request request, Response response) throws Exception {
        String guid = RouteUtils.queryParam(request, "guid");

        Optional<Key> key = keyAccess.lookupPendingKey(guid);
        if (!key.isPresent()) return "Did not find GUID";
        keyAccess.removePendingKey(guid);
        keyAccess.insertKey(key.get().getUserId(), key.get().getPublicKey(), key.get().encryptedPrivateKey);
        return "ok";
    }

    String addUserKey(Request request, Response response) throws Exception {
        byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
        String privateKey = RouteUtils.queryParam(request, "privatekey");
        String password = RouteUtils.queryParam(request, "password");
        String guid = nextGUID(random);
        String link = request.url() + "/addkey" + "?guid=" + guid;

        User user = routeUtils.forceLoggedInUser(request);

        if (!user.checkPassword(password)) {
            // user mistyped password
            RouteUtils.errorMessage(request, "Incorrect password");
            response.redirect("/user/" + user.getUsername());
            return "redirected";
        }

        boolean validKey = true;
        try {
            ECDSAPublicKey.DESERIALIZER.deserialize(publicKey);
        } catch (DeserializationException | IOException e) {
            validKey = false;
        }

        if (validKey) {
            String email = user.getEmail();
            mailService.sendEmail(email, SUBJECT, emailBody(link));
            RouteUtils.successMessage(request, "Check your inbox.");
            keyAccess.insertPendingKey(user.getId(), publicKey, privateKey, guid);
        }

        else {
            RouteUtils.errorMessage(request, "Invalid public key");
        }
        response.redirect("/user/" + user.getUsername());
        return "redirected";

    }

    String deleteKey(Request request, Response response) throws Exception {
        byte[] publicKey = RouteUtils.queryParamHex(request, "publickey");
        User user = routeUtils.forceLoggedInUser(request);
        Optional<Key> optKey = keyAccess.getKey(user.getId(), publicKey);
        if (optKey.isPresent()) {
            keyAccess.deleteKey(optKey.get().getId());
        }
        return "ok";
    }


    // TODO: Don't duplicate these.
    private static String emailBody(String link) {
        return String.format(
                "Click on the link below to verify new key.%n%n%s",
                link
        );
    }
}
