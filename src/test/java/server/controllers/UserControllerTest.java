package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.ECDSAPublicKey;
import message.Message;
import message.payloads.GetFundsRequestPayload;
import message.payloads.GetFundsResponsePayload;
import org.junit.Assert;
import org.junit.Test;
import server.access.UserAccess;
import server.models.User;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;

import java.util.Map;
import java.util.stream.Collectors;

import static testutils.TestUtils.assertPresent;

public class UserControllerTest extends ControllerTest {
    private UserAccess userAccess;
    private UserController controller;
    private final Fixtures fixtures;
    private final MockCryptocurrencyEndpoint endpoint;

    public UserControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(UserController.class);
        controller.init();
        userAccess = injector.getInstance(UserAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = injector.getInstance(Fixtures.class);
        endpoint = injector.getInstance(MockCryptocurrencyEndpoint.Provider.class).getEndpoint();
    }

    @Test
    public void testRegisterValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::register).handle(request, mockResponse.get());
        Assert.assertTrue(mockResponse.redirected());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        Assert.assertEquals(request.session().attribute("username"), "newUsername");
        assertPresent(userAccess.getUserByUsername("newUsername"));
        assertPresent(userAccess.getUserByEmail("newuser@example.com"));
    }

    @Test
    public void testRegisterInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", ":(")
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::register).handle(request, mockResponse.get());
        Assert.assertEquals("/register", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserByEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testRegisterInvalidPassword() throws Exception {
        Assert.assertNotNull(controller);
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", "nogood")
                .addQueryParam("confirm", "nogood")
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::register).handle(request, mockResponse.get());
        Assert.assertEquals(400, mockResponse.status());
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserByEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testRegisterTakenUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", fixtures.user(1).getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::register).handle(request, mockResponse.get());
        Assert.assertEquals("/register", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserByEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testLoginValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", fixtures.user(1).getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::login).handle(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        Assert.assertEquals(fixtures.user(1).getUsername(), request.session().attribute("username"));
    }

    @Test
    public void testLoginInvalidPassword() throws Exception {
        MockResponse mockResponse = invalidLogin(fixtures.user(1).getUsername());
        Assert.assertEquals("/login", mockResponse.redirectedTo());
    }

    @Test
    public void testLoginInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", "invalidUsername")
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::login).handle(request, mockResponse.get());
        Assert.assertEquals("/login", mockResponse.redirectedTo());
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testLoginLockout() throws Exception {
        for (int i = 0; i < UserController.FAILED_LOGIN_LIMIT - 1; i++) {
            MockResponse mockResponse = invalidLogin(fixtures.user(1).getUsername());
            Assert.assertEquals("/login", mockResponse.redirectedTo());
        }
        User user = assertPresent(userAccess.getUserByUsername(fixtures.user(1).getUsername()));
        Assert.assertEquals(UserController.FAILED_LOGIN_LIMIT - 1, user.getFailedLogins());
        MockResponse mockResponse = invalidLogin(fixtures.user(1).getUsername());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
    }

    @Test
    public void testLogout() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        Response response = new MockResponse().get();
        route(controller::logout).handle(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testViewUser() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::viewUser).handle(request, response);
        Assert.assertEquals("user.ftl", modelAndView.getViewName());
    }

    @Test
    public void testAddFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .addQueryParam("friend", fixtures.user(1).getUsername())
                .get();
        Response response = new MockResponse().get();
        route(controller::addFriend).handle(request, response);
    }

    @Test
    public void testDeleteFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .addQueryParam("friend", fixtures.user(1).getUsername())
                .get();
        Response response = new MockResponse().get();
        route(controller::deleteFriend).handle(request, response);
    }

    @Test
    public void testBalance() throws Exception {
        endpoint.respondWith(message -> {
            try {
                Assert.assertEquals(Message.GET_FUNDS, message.type);
                GetFundsRequestPayload request = GetFundsRequestPayload.DESERIALIZER.deserialize(message.payload);
                Map<ECDSAPublicKey, Long> balances = request.requestedKeys.stream()
                        .collect(Collectors.toMap(k -> k, k -> 100L));
                GetFundsResponsePayload response = new GetFundsResponsePayload(balances);
                return response.toMessage();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
                return null;
            }
        });

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = template(controller::balance).handle(request, response);
        Assert.assertEquals("balance.ftl", modelAndView.getViewName());
    }

    private MockResponse invalidLogin(String username) throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", username)
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::login).handle(request, mockResponse.get());
        Assert.assertNull(request.attribute("username"));
        return mockResponse;
    }
}
