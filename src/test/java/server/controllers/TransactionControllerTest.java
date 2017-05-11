package server.controllers;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.ECDSAPublicKey;
import message.IncomingMessage;
import message.Message;
import message.payloads.GetUTXWithKeysResponsePayload;
import network.ConnectionThread;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import server.access.TransactionAccess;
import server.bodies.SendTransactionBody;
import server.bodies.SignatureBody;
import server.bodies.TransactionResponseBody;
import server.models.Key;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Constants;
import server.utils.RouteUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;
import transaction.Transaction;
import utils.ByteUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static testutils.TestUtils.assertPresent;
import static testutils.TestUtils.assertThrows;

@RunWith(JUnitQuickcheck.class)
public class TransactionControllerTest extends ControllerTest {
    private TransactionController controller;
    private TransactionAccess access;
    private Fixtures fixtures;

    public TransactionControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
        access = injector.getInstance(TransactionAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = injector.getInstance(Fixtures.class);
    }

    @Test
    public void testGetTransact() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = controller.getTransact(request, mockResponse.get());
        assertEquals("transact.ftl", modelAndView.getViewName());
    }

    @Property(trials = 1)
    public void testTransactValid(Transaction transaction) throws Exception {
        final int senderId = 1;
        final int recipientId = 2;
        ServerSocket socket = new ServerSocket(0);
        Constants.setNodeAddress(new InetSocketAddress(
                InetAddress.getLocalHost(),
                socket.getLocalPort()
        ));

        final Key key = fixtures.keyOwnedBy(senderId);
        BlockingQueue<IncomingMessage> messageQueue = new ArrayBlockingQueue<>(10);
        new Thread(() -> {
            try {
                ConnectionThread connectionThread = new ConnectionThread(socket.accept(), messageQueue);
                connectionThread.start();
                IncomingMessage message = messageQueue.take();
                assertEquals(Message.GET_UTX_WITH_KEYS, message.type);
                ECDSAPublicKey ecKey = assertPresent(key.asKey());
                GetUTXWithKeysResponsePayload response = GetUTXWithKeysResponsePayload.success(
                        Collections.singletonList(ecKey),
                        transaction
                );
                connectionThread.send(response.toMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user(recipientId).getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "test message")
                .addSessionAttribute("username", fixtures.user(senderId).getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        String json = controller.transact(request, mockResponse.get());
        TransactionResponseBody body = new Gson().fromJson(json, TransactionResponseBody.class);
        assertEquals(1, body.encryptedKeys.size());
        assertEquals(key.encryptedPrivateKey, body.encryptedKeys.get(0));
    }

    @Test
    public void testTransactInvalid() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        Constants.setNodeAddress(new InetSocketAddress(
                InetAddress.getLocalHost(),
                socket.getLocalPort()
        ));

        BlockingQueue<IncomingMessage> messageQueue = new ArrayBlockingQueue<>(10);
        new Thread(() -> {
            try {
                ConnectionThread connectionThread = new ConnectionThread(socket.accept(), messageQueue);
                connectionThread.start();
                IncomingMessage message = messageQueue.take();
                assertEquals(Message.GET_UTX_WITH_KEYS, message.type);
                GetUTXWithKeysResponsePayload response = GetUTXWithKeysResponsePayload.failure();
                connectionThread.send(response.toMessage());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user(1).getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "this is a test message")
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.transact(request, response); // TODO check return value
    }

    @Property(trials = 1)
    public void testSendTransaction(Transaction transaction) throws Exception {
        ServerSocket socket = new ServerSocket(0);
        Constants.setNodeAddress(new InetSocketAddress(
                InetAddress.getLocalHost(),
                socket.getLocalPort()
        ));

        BlockingQueue<IncomingMessage> messageQueue = new ArrayBlockingQueue<>(10);
        new Thread(() -> {
            try {
                ConnectionThread connectionThread = new ConnectionThread(socket.accept(), messageQueue);
                connectionThread.start();
                IncomingMessage message = messageQueue.take();
                assertEquals(Message.TRANSACTION, message.type);
                Assert.assertArrayEquals(
                        ByteUtil.asByteArray(transaction::serialize),
                        message.payload
                );
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        byte[] payload = ByteUtil.asByteArray(transaction::serializeWithoutSignatures);
        SendTransactionBody body = new SendTransactionBody(
                ByteUtil.bytesToHexString(payload),
                transaction.signatures()
                        .map(sig -> new SignatureBody(sig.r.toString(16), sig.s.toString(16)))
                        .collect(Collectors.toList())
        );

        Request request = new MockRequest()
                .jsonBody(body)
                .get();
        request.session().attribute("username", "username");

        Response response = new MockResponse().get();
        String resp = controller.sendTransaction(request, response); // TODO check return value
        assertEquals("ok", resp); // TODO this is temporary
    }

    @Test
    public void testSendTransactionInvalidBody() throws Exception {
        Request request = new MockRequest()
                .setBody(randomAsciiString(1024))
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        Response response = new MockResponse().get();
        assertThrows(errorMessage,
                () -> controller.sendTransaction(request, response),
                RouteUtils.InvalidParamException.class
        );
    }

    @Test
    public void testGetRequests() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = controller.getRequests(request, mockResponse.get());
        assertEquals("request.ftl", modelAndView.getViewName());
    }

    @Test
    public void testCreateRequest() throws Exception {
        final long amount = (long) (1 + random.nextInt(1000));
        final String message = "Transaction message";
        final String sender = fixtures.user(2).getUsername();
        final String recipient = fixtures.user(1).getUsername();
        Request request = new MockRequest()
                .addSessionAttribute("username", recipient)
                .addQueryParam("requestee", sender)
                .addQueryParam("amount", Long.toString(amount))
                .addQueryParam("message", message)
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.createRequest(request, mockResponse.get());
        boolean requestAdded = access.getRequests(sender).stream()
                .anyMatch(t -> t.isRequest()
                        && t.getTouser().equals(recipient)
                        && t.getMessage().equals(message)
                        && t.getAmount() == amount);
        Assert.assertTrue(requestAdded);
    }

    @Test
    public void testDeleteRequest() throws Exception {
        final long amount = 100;
        final String message = "Message";
        final User sender = fixtures.user(1);
        final User recipient = fixtures.user(2);

        access.insertTransaction(sender.getUsername(), recipient.getUsername(), amount, message, true);

        server.models.Transaction transaction = assertPresent(
                access.getRequests(sender.getUsername()).stream()
                        .filter(t -> t.isRequest()
                                && t.getTouser().equals(recipient.getUsername())
                                && t.getMessage().equals(message)
                                && t.getAmount() == amount)
                        .findFirst()
        );
        Request request = new MockRequest()
                .addSessionAttribute("username", sender.getUsername())
                .addQueryParam("tranid", Integer.toString(transaction.getTranid()))
                .get();
        MockResponse mockResponse = new MockResponse();
        controller.deleteRequest(request, mockResponse.get());

        Assert.assertFalse(access.getRequests(sender.getUsername()).stream()
                .anyMatch(t -> t.getTranid() == transaction.getTranid())
        );
    }
}
