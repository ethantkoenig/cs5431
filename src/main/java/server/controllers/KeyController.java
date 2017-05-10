package server.controllers;

import com.google.inject.Inject;
import crypto.Crypto;
import crypto.ECDSAPublicKey;
import server.access.KeyAccess;
import server.bodies.KeyBody;
import server.bodies.KeysBody;
import server.models.Key;
import server.models.User;
import server.utils.MailService;
import server.utils.RouteUtils;
import spark.Request;
import spark.Response;
import utils.ByteUtil;
import utils.Config;
import utils.DeserializationException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static server.utils.RouteUtils.wrapRoute;
import static spark.Spark.*;

public class KeyController extends AbstractController {
    private static final String SUBJECT = "Yaccoin New Key";

    private final KeyAccess keyAccess;
    private final RouteUtils routeUtils;
    private final MailService mailService;
    private final Crypto crypto;

    @Inject
    public KeyController(KeyAccess keyAccess,
                         RouteUtils routeUtils,
                         MailService mailService,
                         Crypto crypto) {
        this.keyAccess = keyAccess;
        this.routeUtils = routeUtils;
        this.mailService = mailService;
        this.crypto = crypto;
    }

    public void init() {
        path("/keys", () -> {
            get("", wrapRoute(this::getKeys));
            post("", wrapRoute(this::addUserKey));
            delete("", wrapRoute(this::deleteKey));
            get("/addkey", wrapRoute(this::finalizeInsertKey));
        });
    }

    String getKeys(Request request, Response response) throws Exception {
        User user = routeUtils.forceLoggedInUser(request);
        List<KeyBody> keys = keyAccess.getKeysByUserID(user.getId()).stream()
                .map(key -> {
                    String publicKey = ByteUtil.bytesToHexString(key.getPublicKey());
                    return new KeyBody(publicKey, key.encryptedPrivateKey);
                }).collect(Collectors.toList());
        return routeUtils.toJson(response, new KeysBody(keys));
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
        String guid = crypto.nextGUID();
        String link = request.url() + "/addkey" + "?guid=" + guid;

        User user = routeUtils.forceLoggedInUser(request);

        if (!user.checkPassword(password)) {
            // user mistyped password
            RouteUtils.errorMessage(request, "Incorrect password");
            response.redirect("/user");
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
        response.redirect("/user");
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
