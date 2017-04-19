package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import network.*;
import org.junit.Assert;
import org.mockito.Mockito;
import server.access.DatabaseUserAccess;
import server.access.UserAccess;
import server.utils.Constants;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;
import testutils.TestUtils;
import utils.ByteUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UserControllerTest extends ControllerTest {
    private UserAccess userAccess;
    private UserController controller;
    private final Fixtures fixtures;

    public UserControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(UserController.class);
        controller.init();
        userAccess = injector.getInstance(DatabaseUserAccess.class);
        fixtures = new Fixtures();
    }

    public void testRegisterValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();
        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.register(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "register.ftl");
        Assert.assertEquals(request.session().attribute("username"), "newUsername");
        TestUtils.assertPresent(userAccess.getUserbyUsername("newUsername"));
        TestUtils.assertPresent(userAccess.getUserbyEmail("newuser@example.com"));
    }

    public void testRegisterInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", ":(")
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.register(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "register.ftl");
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    public void testRegisterInvalidPassword() throws Exception {
        Assert.assertNotNull(controller);
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", "nogood")
                .addQueryParam("confirm", "nogood")
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.register(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "register.ftl");
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    public void testRegisterTakenUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .addQueryParam("confirm", Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.register(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "register.ftl");
        Assert.assertNull(request.session().attribute("username"));
        Assert.assertFalse(userAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    public void testLoginValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.login(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "user.ftl");

        Assert.assertEquals(fixtures.user.getUsername(), request.session().attribute("username"));
    }

    public void testLoginInvalidPassword() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", fixtures.user.getUsername())
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.login(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "login.ftl");
        Assert.assertNull(request.attribute("username"));
    }

    public void testLoginInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", "invalidUsername")
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.login(request, response);
        Assert.assertEquals(modelAndView.getViewName(), "login.ftl");
        Assert.assertNull(request.attribute("username"));
    }

    public void testLogout() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.logout(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    public void testViewUser() throws Exception {
        Request request = new MockRequest()
                .addParam(":name", fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.viewUser(request, response);
    }

    public void testAddUserKey() throws Exception {
        ECDSAKeyPair pair = Crypto.signatureKeyPair();
        byte[] publicBytes = ByteUtil.asByteArray(pair.publicKey::serialize);
        String privateKey = ByteUtil.bytesToHexString(ByteUtil.asByteArray(pair.privateKey::serialize));

        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addQueryParam("privatekey", privateKey)
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.addUserKey(request, response);
        TestUtils.assertPresent(userAccess.getKey(fixtures.user.getId(), publicBytes));
    }

    public void testRemoveUserKey() throws Exception {
        byte[] publicBytes = ByteUtil.asByteArray(fixtures.key::serialize);
        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.deleteKey(request, response);
        Assert.assertFalse(userAccess.getKey(fixtures.user.getId(), publicBytes).isPresent());
    }

    public void testAddFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("friend", fixtures.user.getUsername())
                .get();
        Response response = Mockito.mock(Response.class);
        controller.addFriend(request, response);
    }

    public void testDeleteFriend() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user.getUsername())
                .addQueryParam("friend", fixtures.user.getUsername())
                .get();
        Response response = Mockito.mock(Response.class);
        controller.deleteFriend(request, response);
    }

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
                GetFundsRequest request = GetFundsRequest.DESERIALIZER.deserialize(message.payload);
                Map<ECDSAPublicKey, Long> balances = new HashMap<>();
                for (ECDSAPublicKey key : request.requestedKeys) {
                    balances.put(key, 100L);
                }
                GetFundsResponse response = new GetFundsResponse(balances);
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
        Response response = Mockito.mock(Response.class);
        ModelAndView modelAndView = controller.balance(request, response);
        Assert.assertEquals("balance.ftl", modelAndView.getViewName());
    }
}
