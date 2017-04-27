package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import server.access.PasswordRecoveryAccess;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;
import testutils.MockResponse;

public class PasswordRecoveryControllerTest extends ControllerTest {

    private PasswordRecoveryController controller;
    private UserController userController;
    private PasswordRecoveryAccess access;
    private Fixtures fixtures;

    public PasswordRecoveryControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(PasswordRecoveryController.class);
        controller.init();
        userController = injector.getInstance(UserController.class);
        access = injector.getInstance(PasswordRecoveryAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
    }

    @Test
    public void testGetRecoverNoGUID() throws Exception {
        Request request = new MockRequest().get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getRecover(request, response);
        Assert.assertEquals("recover.ftl", modelAndView.getViewName());
    }

    @Test
    public void testGetRecoverGUID() throws Exception {
        final String guid = "guid1";
        access.insertPasswordRecovery(fixtures.user.getId(), guid);

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("guid", guid)
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.getRecover(request, response);
        Assert.assertEquals("resetpass.ftl", modelAndView.getViewName());
    }

    @Test
    public void testPostRecover() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "example@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.postRecover(request, mockResponse.get());
        Assert.assertEquals("/recover", mockResponse.redirectedTo());
    }

    @Test
    public void testPostRecoverBadAddress() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "nonexistent@example.com")
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.postRecover(request, mockResponse.get());
        Assert.assertEquals("/recover", mockResponse.redirectedTo());
    }

    @Test
    public void testReset() throws Exception {
        final String guid = "guid1";
        access.insertPasswordRecovery(fixtures.user.getId(), guid);

        final String newPassword = randomShaTwoFiftySix().toString();
        Request request = new MockRequest()
                .addQueryParam("password", newPassword)
                .addQueryParam("passwordConfirm", newPassword)
                .addQueryParam("guid", guid)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.reset(request, mockResponse.get());
        Assert.assertEquals("/login", mockResponse.redirectedTo());

        // should be able to login with new password
        request = new MockRequest()
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", newPassword)
                .get();
        mockResponse = new MockResponse();
        userController.login(request, mockResponse.get());
        Assert.assertEquals("/user/" + fixtures.user.getUsername(), mockResponse.redirectedTo());

        Assert.assertEquals(fixtures.user.getUsername(), request.session().attribute("username"));
    }
}
