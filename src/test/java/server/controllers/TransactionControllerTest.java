package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.GetUTXWithKeysResponsePayload;
import network.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import server.utils.ConnectionProvider;
import server.utils.Constants;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.Fixtures;
import testutils.MockRequest;
import testutils.MockResponse;
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

@RunWith(JUnitQuickcheck.class)
public class TransactionControllerTest extends ControllerTest {
    private TransactionController controller;
    private Fixtures fixtures;

    public TransactionControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = new Fixtures();
    }

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
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
        String r = transaction.signatures().map(sig -> sig.r).map(i -> i.toString(16))
                .collect(Collectors.joining(","));
        String s = transaction.signatures().map(sig -> sig.s).map(i -> i.toString(16))
                .collect(Collectors.joining(","));

        Request request = new MockRequest()
                .addQueryParamHex("payload", payload)
                .addQueryParam("r", r)
                .addQueryParam("s", s)
                .get();
        request.session().attribute("username", "username");

        Response response = new MockResponse().get();
        String resp = controller.sendTransaction(request, response); // TODO check return value
        Assert.assertEquals("ok", resp); // TODO this is temporary
    }
}
