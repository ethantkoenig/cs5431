package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import server.access.AccountRecoveryAccess;
import server.access.UserAccess;
import server.models.User;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;
import testutils.MockResponse;

import static testutils.TestUtils.assertPresent;

public class AccountRecoveryControllerTest extends ControllerTest {

    private AccountRecoveryController controller;
    private AccountRecoveryAccess access;
    private UserAccess userAccess;
    private Fixtures fixtures;

    public AccountRecoveryControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(AccountRecoveryController.class);
        controller.init();
        access = injector.getInstance(AccountRecoveryAccess.class);
        userAccess = injector.getInstance(UserAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
    }

    @Test
    public void testGetResetNoGUID() throws Exception {
        Request request = new MockRequest().get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getReset(request, response);
        Assert.assertEquals("resetRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetResetGUID() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("guid", guid)
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getReset(request, response);
        Assert.assertEquals("reset.ftl", modelAndView.getViewName());
    }

    @Test
    public void testResetMail() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "example@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.resetMail(request, mockResponse.get());
        Assert.assertEquals("/reset", mockResponse.redirectedTo());
    }

    @Test
    public void testResetMailBadAddress() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "nonexistent@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.resetMail(request, mockResponse.get());
        Assert.assertEquals("/reset", mockResponse.redirectedTo());
    }

    @Test
    public void testReset() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        final String newPassword = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("password", newPassword)
                .addQueryParam("passwordConfirm", newPassword)
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.reset(request, mockResponse.get());
        Assert.assertEquals("/login", mockResponse.redirectedTo());

        User user = assertPresent(userAccess.getUserByID(fixtures.user.getId()));
        Assert.assertTrue(user.checkPassword(newPassword));
    }

    @Test
    public void testGetUnlockNoGUID() throws Exception {
        Request request = new MockRequest().get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getUnlock(request, response);
        Assert.assertEquals("unlockRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetUnlockBadGUID() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("guid", randomShaTwoFiftySix().toString())
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getUnlock(request, response);
        Assert.assertEquals("unlockRequest.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetUnlock() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("guid", guid)
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getUnlock(request, response);
        Assert.assertEquals("unlock.ftl", modelAndView.getViewName());
    }

    @Test
    public void testUnlockMail() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("email", fixtures.user.getEmail())
                .addQueryParam("guid", guid)
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.unlockMail(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
    }

    @Test
    public void testUnlock() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.unlock(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        User user = assertPresent(userAccess.getUserByID(fixtures.user.getId()));
        Assert.assertEquals(0, user.getFailedLogins());
        Assert.assertEquals(fixtures.user.getUsername(), request.session().attribute("username"));
    }

    @Test
    public void testUnlockBadGUID() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("guid", randomShaTwoFiftySix().toString())
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.unlock(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
    }

    @Test
    public void testUnlockBadPassword() throws Exception {
        final String guid = randomShaTwoFiftySix().toString();
        access.insertRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addQueryParam("password", randomShaTwoFiftySix().toString())
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.unlock(request, mockResponse.get());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
    }
}
