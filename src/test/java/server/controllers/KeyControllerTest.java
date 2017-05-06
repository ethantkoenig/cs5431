package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import org.junit.Assert;
import org.junit.Test;
import server.access.KeyAccess;
import server.utils.ConnectionProvider;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;
import testutils.MockResponse;
import utils.ByteUtil;

public class KeyControllerTest extends ControllerTest {
    private KeyAccess keyAccess;
    private KeyController controller;
    private final Fixtures fixtures;

    public KeyControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(KeyController.class);
        controller.init();
        keyAccess = injector.getInstance(KeyAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
    }

    @Test
    public void testAddUserKey() throws Exception {
        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        byte[] publicBytes = ByteUtil.asByteArray(pair.publicKey::serialize);
        String privateKey = ByteUtil.bytesToHexString(ByteUtil.asByteArray(pair.privateKey::serialize));

        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addQueryParam("privatekey", privateKey)
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.addUserKey(request, response);
        Assert.assertFalse(keyAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
    }

    @Test
    public void testRemoveUserKey() throws Exception {
        byte[] publicBytes = ByteUtil.asByteArray(fixtures.key::serialize);
        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.deleteKey(request, response);
        Assert.assertFalse(keyAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
    }

    @Test
    public void testFinalizeInsertKey() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        byte[] publicBytes = ByteUtil.asByteArray(pair.publicKey::serialize);
        String privateKey = ByteUtil.bytesToHexString(ByteUtil.asByteArray(pair.privateKey::serialize));

        keyAccess.insertPendingKey(fixtures.user.getId(), publicBytes, privateKey, guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();

        Response response = new MockResponse().get();
        controller.finalizeInsertKey(request, response);

        Assert.assertFalse(keyAccess.lookupPendingKey(guid).isPresent());
    }
}
