package server.controllers;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.ECDSAKeyPair;
import org.junit.Assert;
import org.junit.Test;
import server.access.KeyAccess;
import server.bodies.KeysBody;
import server.models.Key;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;

import java.net.URL;

import static testutils.MockMailService.getParam;
import static testutils.MockMailService.getURL;
import static testutils.TestUtils.assertPresent;
import static utils.ByteUtil.asByteArray;
import static utils.ByteUtil.bytesToHexString;

public class KeyControllerTest extends ControllerTest {
    private final KeyAccess keyAccess;
    private final KeyController controller;
    private final Fixtures fixtures;
    private final MockMailService mailService;

    public KeyControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(KeyController.class);
        controller.init();
        keyAccess = injector.getInstance(KeyAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = injector.getInstance(Fixtures.class);
        mailService = injector.getInstance(MockMailService.class);
    }

    @Test
    public void testGetKeys() throws Exception {
        final int userId = 1;
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(userId).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        Object json = route(controller::getKeys).handle(request, mockResponse.get());
        KeysBody keysBody = new Gson().fromJson(json.toString(), KeysBody.class);
        Assert.assertEquals(1, keysBody.keys.size());
        Assert.assertEquals(
                bytesToHexString(asByteArray(fixtures.ecKeyOwnedBy(userId)::serialize)),
                keysBody.keys.get(0).publicKey
        );
    }

    @Test
    public void testAddUserKey() throws Exception {
        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addQueryParam("privatekey", privateKey)
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        Response response = new MockResponse().get();
        route(controller::addUserKey).handle(request, response);
        URL guidURL = getURL(mailService.assertMailToGetBody(fixtures.user(1).getEmail()));
        String guid = getParam(guidURL, "guid");
        Key pendingKey = assertPresent(keyAccess.lookupPendingKey(guid));
        Assert.assertEquals(privateKey, pendingKey.encryptedPrivateKey);
        Assert.assertFalse(keyAccess.getKey(fixtures.user(1).getId(), publicBytes).isPresent());
    }

    @Test
    public void testAddUserKeyWrongPassword() throws Exception {
        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addQueryParam("privatekey", privateKey)
                .addQueryParam("password", randomShaTwoFiftySix().toString())
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::addUserKey).handle(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        Assert.assertFalse(mailService.sentTo(fixtures.user(1).getUsername()));
        Assert.assertFalse(keyAccess.getKey(fixtures.user(1).getId(), publicBytes).isPresent());
    }

    @Test
    public void testRemoveUserKey() throws Exception {
        final int userId = 1;
        byte[] publicBytes = asByteArray(fixtures.ecKeyOwnedBy(userId)::serialize);
        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addSessionAttribute("username", fixtures.user(userId).getUsername())
                .get();

        Response response = new MockResponse().get();
        route(controller::deleteKey).handle(request, response);
        Assert.assertFalse(keyAccess.getKey(userId, publicBytes).isPresent());
    }

    @Test
    public void testGetAddKey() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();

        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        keyAccess.insertPendingKey(fixtures.user(1).getId(), publicBytes, privateKey, guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = template(controller::getAddKey).handle(request, mockResponse.get());
        Assert.assertEquals("finalizeKey.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetAddKeyBadGuid() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        template(controller::getAddKey).handle(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
    }

    @Test
    public void testFinalizeKeyInsert() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();

        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        keyAccess.insertPendingKey(fixtures.user(1).getId(), publicBytes, privateKey, guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::finalizeKeyInsert).handle(request, mockResponse.get());
        Assert.assertFalse(keyAccess.lookupPendingKey(guid).isPresent());
        assertPresent(keyAccess.getKey(fixtures.user(1).getId(), publicBytes));
    }

    @Test
    public void testFinalizeKeyInsertBadGuid() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::finalizeKeyInsert).handle(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
    }
}
