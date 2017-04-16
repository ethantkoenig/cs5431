package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.Crypto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;
import spark.Response;
import testutils.MockRequest;
import testutils.MockUserAccess;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.ByteUtil;

public class UserControllerTest extends RandomizedTest {
    private UserController controller;
    private MockUserAccess mockUserAccess;

    @Before
    public void initController() throws Exception {
        MockUserAccess.reset();
        mockUserAccess = MockUserAccess.get();
        Injector injector = Guice.createInjector(new MockUserAccess.Model());
        controller = injector.getInstance(UserController.class);
        controller.init();
    }

    @Test
    public void testRegisterValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", MockUserAccess.Fixtures.USER_PASSWORD)
                .get();
        Response response = Mockito.mock(Response.class);
        controller.register(request, response);

        TestUtils.assertPresent(mockUserAccess.getUserbyUsername("newUsername"));
        TestUtils.assertPresent(mockUserAccess.getUserbyEmail("newuser@example.com"));
    }

    @Test
    public void testRegisterInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", ":(")
                .addQueryParam("password", MockUserAccess.Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        controller.register(request, response);
        Assert.assertFalse(mockUserAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testRegisterInvalidPassword() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", "newUsername")
                .addQueryParam("password", "nogood")
                .get();

        Response response = Mockito.mock(Response.class);
        controller.register(request, response);
        Assert.assertFalse(mockUserAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testRegisterTakenUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("email", "newuser@example.com")
                .addQueryParam("username", mockUserAccess.fixtures.user.getUsername())
                .addQueryParam("password", MockUserAccess.Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        controller.register(request, response);
        Assert.assertFalse(mockUserAccess.getUserbyEmail("newuser@example.com").isPresent());
    }

    @Test
    public void testLoginValid() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", mockUserAccess.fixtures.user.getUsername())
                .addQueryParam("password", MockUserAccess.Fixtures.USER_PASSWORD)
                .get();

        Response response = Mockito.mock(Response.class);
        controller.login(request, response);
        Assert.assertEquals(mockUserAccess.fixtures.user.getUsername(), request.session().attribute("username"));
    }

    @Test
    public void testLoginInvalidPassword() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", mockUserAccess.fixtures.user.getUsername())
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        Response response = Mockito.mock(Response.class);
        controller.login(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testLoginInvalidUsername() throws Exception {
        Request request = new MockRequest()
                .addQueryParam("username", "invalidUsername")
                .addQueryParam("password", "wr0ngP@ssword!!")
                .get();

        Response response = Mockito.mock(Response.class);
        controller.login(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testLogout() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", mockUserAccess.fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.logout(request, response);
        Assert.assertNull(request.attribute("username"));
    }

    @Test
    public void testViewUser() throws Exception {
        Request request = new MockRequest()
                .addParam(":name", mockUserAccess.fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.viewUser(request, response);
    }

    @Test
    public void testAddUserKey() throws Exception {
        byte[] publicBytes = ByteUtil.asByteArray(
                Crypto.signatureKeyPair().publicKey::serialize
        );
        String privateKey = randomAsciiString(100);

        Request request = new MockRequest()
                .addQueryParamHex("publickey", publicBytes)
                .addQueryParam("privatekey", privateKey)
                .addSessionAttribute("username", mockUserAccess.fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.addUserKey(request, response);
        TestUtils.assertPresent(mockUserAccess.getKey(1, publicBytes));
    }
}
