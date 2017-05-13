package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import server.access.AccountRecoveryAccess;
import server.access.UserAccess;
import server.bodies.KeyBody;
import server.bodies.KeysBody;
import server.models.User;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;
import utils.ByteUtil;

import java.util.Collections;

import static testutils.TestUtils.assertPresent;

public class AccountRecoveryControllerTest extends ControllerTest {

    private AccountRecoveryController controller;
    private AccountRecoveryAccess access;
    private UserAccess userAccess;
    private Fixtures fixtures;

    public AccountRecoveryControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(AccountRecoveryController.class);
        controller.init();
        access = injector.getInstance(AccountRecoveryAccess.class);
        userAccess = injector.getInstance(UserAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = injector.getInstance(Fixtures.class);
    }

    @Test
    public void testGetResetNoGUID() throws Exception {
        Request request = new MockRequest().get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::getReset).handle(request, response);
        Assert.assertEquals("resetRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetResetGUID() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .addQueryParam("guid", guid)
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::getReset).handle(request, response);
        Assert.assertEquals("reset.ftl", modelAndView.getViewName());
    }

    @Test
    public void testResetMail() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "example@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::resetMail).handle(request, mockResponse.get());
        Assert.assertEquals("/reset", mockResponse.redirectedTo());
    }

    @Test
    public void testResetMailBadAddress() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "nonexistent@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::resetMail).handle(request, mockResponse.get());
        Assert.assertEquals("/reset", mockResponse.redirectedTo());
    }

    @Test
    public void testReset() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        final String newPassword = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("password", newPassword)
                .addQueryParam("passwordConfirm", newPassword)
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::reset).handle(request, mockResponse.get());
        Assert.assertEquals("/login", mockResponse.redirectedTo());

        User user = assertPresent(userAccess.getUserByID(fixtures.user(1).getId()));
        Assert.assertTrue(user.checkPassword(newPassword));
    }

    @Test
    public void testGetUnlockNoGUID() throws Exception {
        Request request = new MockRequest().get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::getUnlock).handle(request, response);
        Assert.assertEquals("unlockRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetUnlockBadGUID() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("guid", randomShaTwoFiftySix().toString())
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::getUnlock).handle(request, response);
        Assert.assertEquals("unlockRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetUnlock() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::getUnlock).handle(request, response);
        Assert.assertEquals("unlock.ftl", modelAndView.getViewName());
    }

    @Test
    public void testUnlockMail() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("email", fixtures.user(1).getEmail())
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::unlockMail).handle(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
    }

    @Test
    public void testUnlock() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::unlock).handle(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        User user = assertPresent(userAccess.getUserByID(fixtures.user(1).getId()));
        Assert.assertEquals(0, user.getFailedLogins());
        Assert.assertEquals(fixtures.user(1).getUsername(), request.session().attribute("username"));
    }

    @Test
    public void testUnlockBadGUID() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("guid", randomShaTwoFiftySix().toString())
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::unlock).handle(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
    }

    @Test
    public void testUnlockBadPassword() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("password", randomShaTwoFiftySix().toString())
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::unlock).handle(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
    }

    @Test
    public void testGetChangePasswordNoGuid() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = template(controller::getChangePassword).handle(request, mockResponse.get());
        Assert.assertEquals("changePasswordRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetChangePasswordGuid() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = template(controller::getChangePassword).handle(request, mockResponse.get());
        Assert.assertEquals("changePasswordRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testChangePasswordMail() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::changePasswordMail).handle(request, mockResponse.get());
        Assert.assertEquals("/change_password", mockResponse.redirectedTo());
    }

    @Test
    public void testChangePassword() throws Exception {
        final int userId = 1;
        String publicKey = ByteUtil.bytesToHexString(
                ByteUtil.asByteArray(fixtures.ecKeyOwnedBy(userId)::serialize)
        );
        final KeysBody keysBody = new KeysBody(Collections.singletonList(
                new KeyBody(publicKey, randomAsciiString(128))
        ));

        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user(1).getId(), guid);
        final String newPassword = randomShaTwoFiftySix().toString();

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(userId).getUsername())
                .addQueryParam("guid", guid)
                .addQueryParam("password", newPassword)
                .jsonBody(keysBody)
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::changePassword).handle(request, mockResponse.get());

        User user = assertPresent(userAccess.getUserByID(fixtures.user(userId).getId()));
        Assert.assertTrue(user.checkPassword(newPassword));
    }

    @Test
    public void testChangePasswordBadGuid() throws Exception {
        final int userId = 1;
        String publicKey = ByteUtil.bytesToHexString(
                ByteUtil.asByteArray(fixtures.ecKeyOwnedBy(userId)::serialize)
        );
        final KeysBody keysBody = new KeysBody(Collections.singletonList(
                new KeyBody(publicKey, randomAsciiString(128))
        ));

        final String guid = randomShaTwoFiftySix().toString();
        final String newPassword = randomShaTwoFiftySix().toString();

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(userId).getUsername())
                .addQueryParam("guid", guid)
                .addQueryParam("password", newPassword)
                .jsonBody(keysBody)
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::changePassword).handle(request, mockResponse.get());
        Assert.assertEquals(400, mockResponse.status());
        User user = assertPresent(userAccess.getUserByID(fixtures.user(userId).getId()));
        Assert.assertFalse(user.checkPassword(newPassword));
    }
}
