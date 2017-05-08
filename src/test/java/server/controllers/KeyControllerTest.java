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
        fixtures = new Fixtures();
        mailService = injector.getInstance(MockMailService.class);
    }

    @Test
    public void testGetKeys() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        String json = controller.getKeys(request, mockResponse.get());
        KeysBody keysBody = new Gson().fromJson(json, KeysBody.class);
        Assert.assertEquals(1, keysBody.keys.size());
        Assert.assertEquals(
                bytesToHexString(asByteArray(fixtures.key::serialize)),
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
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.addUserKey(request, response);
        URL guidURL = getURL(mailService.assertMailToGetBody(fixtures.user.getEmail()));
        String guid = getParam(guidURL, "guid");
        Key pendingKey = assertPresent(keyAccess.lookupPendingKey(guid));
        Assert.assertEquals(privateKey, pendingKey.encryptedPrivateKey);
        Assert.assertFalse(keyAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
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
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.addUserKey(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        Assert.assertFalse(mailService.sentTo(fixtures.user.getUsername()));
        Assert.assertFalse(keyAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
    }

    @Test
    public void testRemoveUserKey() throws Exception {
        byte[] publicBytes = asByteArray(fixtures.key::serialize);
        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.deleteKey(request, response);
        Assert.assertFalse(keyAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
    }

    @Test
    public void testGetAddKey() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();

        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        keyAccess.insertPendingKey(fixtures.user.getId(), publicBytes, privateKey, guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = controller.getAddKey(request, mockResponse.get());
        Assert.assertEquals("finalizeKey.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetAddKeyBadGuid() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.getAddKey(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
    }

    @Test
    public void testFinalizeKeyInsert() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();

        ECDSAKeyPair pair = crypto.signatureKeyPair();
        byte[] publicBytes = asByteArray(pair.publicKey::serialize);
        String privateKey = bytesToHexString(asByteArray(pair.privateKey::serialize));

        keyAccess.insertPendingKey(fixtures.user.getId(), publicBytes, privateKey, guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.finalizeKeyInsert(request, mockResponse.get());
        Assert.assertFalse(keyAccess.lookupPendingKey(guid).isPresent());
        assertPresent(keyAccess.getKey(fixtures.user.getId(), publicBytes));
    }

    @Test
    public void testFinalizeKeyInsertBadGuid() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.finalizeKeyInsert(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
    }
}
