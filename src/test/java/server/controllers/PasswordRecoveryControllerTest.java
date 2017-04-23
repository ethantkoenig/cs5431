package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import server.access.PasswordRecoveryAccess;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;

public class PasswordRecoveryControllerTest extends ControllerTest {

    private PasswordRecoveryController controller;
    private UserController userController;
    private PasswordRecoveryAccess access;
    private Fixtures fixtures;

    public PasswordRecoveryControllerTest() throws Exception {
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
        Response response = Mockito.mock(Response.class);
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
        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.getRecover(request, response);
        Assert.assertEquals("resetpass.ftl", modelAndView.getViewName());
    }

    @Test
    public void testPostRecover() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "example@example.com")
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.postRecover(request, response);
        Assert.assertEquals("recover.ftl", modelAndView.getViewName());
    }

    @Test
    public void testPostRecoverBadAddress() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "nonexistent@example.com")
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.postRecover(request, response);
        Assert.assertEquals("recover.ftl", modelAndView.getViewName());
    }

    @Test
    public void testReset() throws Exception {
        final String guid = "guid1";
        access.insertPasswordRecovery(fixtures.user.getId(), guid);

        final String newPassword = "newP@ssw0rd!!";
        Request request = new MockRequest()
                .addQueryParam("password", newPassword)
                .addQueryParam("passwordConfirm", newPassword)
                .addQueryParam("guid", guid)
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.reset(request, response);
        Assert.assertEquals("resetpass.ftl", modelAndView.getViewName());

        // should be able to login with new password
        request = new MockRequest()
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", newPassword)
                .get();
        userController.login(request, response);

        Assert.assertEquals(fixtures.user.getUsername(), request.session().attribute("username"));
    }
}
