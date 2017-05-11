package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.ECDSAPublicKey;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.GetFundsRequestPayload;
import message.payloads.GetFundsResponsePayload;
import network.ConnectionThread;
import org.junit.Assert;
import org.junit.Test;
import server.access.UserAccess;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Constants;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;
import utils.ByteUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static server.utils.RouteUtils.wrapRoute;
import static testutils.TestUtils.assertPresent;

public class UserControllerTest extends ControllerTest {
    private UserAccess userAccess;
    private UserController controller;
    private final Fixtures fixtures;

    public UserControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(UserController.class);
        controller.init();
        userAccess = injector.getInstance(UserAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
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
        controller.register(request, mockResponse.get());
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
        controller.register(request, mockResponse.get());
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
        wrapRoute(controller::register).handle(request, mockResponse.get());
        Assert.assertEquals(400, mockResponse.status());
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserByEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testRegisterTakenUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.register(request, mockResponse.get());
        Assert.assertEquals("/register", mockResponse.redirectedTo());
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserByEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testLoginValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.login(request, mockResponse.get());
        Assert.assertEquals("/user", mockResponse.redirectedTo());
        Assert.assertEquals(fixtures.user.getUsername(), request.session().attribute("username"));
    }

    @Test
    public void testLoginInvalidPassword() throws Exception {
        MockResponse mockResponse = invalidLogin(fixtures.user.getUsername());
        Assert.assertEquals("/login", mockResponse.redirectedTo());
    }

    @Test
    public void testLoginInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", "invalidUsername")
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        MockResponse mockResponse = new MockResponse();
        controller.login(request, mockResponse.get());
        Assert.assertEquals("/login", mockResponse.redirectedTo());
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testLoginLockout() throws Exception {
        for (int i = 0; i < UserController.FAILED_LOGIN_LIMIT - 1; i++) {
            MockResponse mockResponse = invalidLogin(fixtures.user.getUsername());
            Assert.assertEquals("/login", mockResponse.redirectedTo());
        }
        User user = assertPresent(userAccess.getUserByUsername(fixtures.user.getUsername()));
        Assert.assertEquals(UserController.FAILED_LOGIN_LIMIT - 1, user.getFailedLogins());
        MockResponse mockResponse = invalidLogin(fixtures.user.getUsername());
        Assert.assertEquals("/unlock", mockResponse.redirectedTo());
    }

    @Test
    public void testLogout() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.logout(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testViewUser() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.viewUser(request, response);
    }

    @Test
    public void testAddFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("friend", fixtures.user.getUsername())
                .get();
        Response response = new MockResponse().get();
        controller.addFriend(request, response);
    }

    @Test
    public void testDeleteFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("friend", fixtures.user.getUsername())
                .get();
        Response response = new MockResponse().get();
        controller.deleteFriend(request, response);
    }

    @Test
    public void testBalance() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        Constants.setNodeAddress(new InetSocketAddress(
                InetAddress.getLocalHost(),
                socket.getLocalPort()
        ));

        new Thread(() -> {
            try {
                BlockingQueue<IncomingMessage> messageQueue = new ArrayBlockingQueue<>(10);
                ConnectionThread connectionThread = new ConnectionThread(socket.accept(), messageQueue);
                connectionThread.start();
                IncomingMessage message = messageQueue.take();
                Assert.assertEquals(Message.GET_FUNDS, message.type);
                GetFundsRequestPayload request = GetFundsRequestPayload.DESERIALIZER.deserialize(message.payload);
                Map<ECDSAPublicKey, Long> balances = new HashMap<>();
                for (ECDSAPublicKey key : request.requestedKeys) {
                    balances.put(key, 100L);
                }
                GetFundsResponsePayload response = new GetFundsResponsePayload(balances);
                connectionThread.send(new OutgoingMessage(Message.FUNDS,
                        ByteUtil.asByteArray(response::serialize))
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();
        Response response = new MockResponse().get();
        ModelAndView modelAndView = controller.balance(request, response);
        Assert.assertEquals("balance.ftl", modelAndView.getViewName());
    }

    private MockResponse invalidLogin(String username) throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", username)
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.login(request, mockResponse.get());
        Assert.assertNull(request.attribute("username"));
        return mockResponse;
    }
}
