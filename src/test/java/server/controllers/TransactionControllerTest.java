package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import message.IncomingMessage;
import message.Message;
import message.payloads.GetUTXWithKeysResponsePayload;
import network.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import server.bodies.SendTransactionBody;
import server.bodies.SignatureBody;
import server.utils.ConnectionProvider;
import server.utils.Constants;
import server.utils.RouteUtils;
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

import static testutils.TestUtils.assertThrows;

@RunWith(JUnitQuickcheck.class)
public class TransactionControllerTest extends ControllerTest {
    private TransactionController controller;
    private Fixtures fixtures;

    public TransactionControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
    }

    @Property(trials = 1)
    public void testTransactValid(Transaction transaction) throws Exception {
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
                Assert.assertEquals(Message.GET_UTX_WITH_KEYS, message.type);
                GetUTXWithKeysResponsePayload response = GetUTXWithKeysResponsePayload.success(
                        Collections.singletonList(fixtures.key),
                        transaction
                );
                connectionThread.send(response.toMessage());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user.getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "test message")
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        controller.transact(request, response); // TODO check return value
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
                Assert.assertEquals(Message.GET_UTX_WITH_KEYS, message.type);
                GetUTXWithKeysResponsePayload response = GetUTXWithKeysResponsePayload.failure();
                connectionThread.send(response.toMessage());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user.getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "this is a test message")
                .addSessionAttribute("username", fixtures.user.getUsername())
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
                Assert.assertEquals(Message.TRANSACTION, message.type);
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
        Assert.assertEquals("ok", resp); // TODO this is temporary
    }

    public void testSendTransactionInvalidBody() throws Exception {
        Request request = new MockRequest()
                .setBody(randomAsciiString(1024))
                .addSessionAttribute("username", fixtures.user.getUsername())
                .get();

        Response response = new MockResponse().get();
        assertThrows(errorMessage,
                () -> controller.sendTransaction(request, response),
                RouteUtils.InvalidParamException.class
        );
    }
}
