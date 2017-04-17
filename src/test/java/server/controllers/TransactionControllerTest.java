package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.Crypto;
import network.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import server.utils.Constants;
import spark.Request;
import spark.Response;
import testutils.ControllerTest;
import testutils.MockRequest;
import transaction.Transaction;
import utils.ByteUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@RunWith(JUnitQuickcheck.class)
public class TransactionControllerTest extends ControllerTest {
    private TransactionController controller = null;

    public TransactionControllerTest() throws SQLException {
        super();
        Injector injector = Guice.createInjector(new Model());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
    }

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    @Property(trials = 1)
    public void testTransactValid() throws Exception {
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
                GetUTXWithKeysResponse response = GetUTXWithKeysResponse.success(
                        new ArrayList<>(),
                        new Transaction.UnsignedBuilder().build()
                );
                connectionThread.send(new OutgoingMessage(Message.UTX_WITH_KEYS,
                        ByteUtil.asByteArray(response::serialize))
                );
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", "username")
                .addQueryParam("amount", "100")
                .get();
        request.session().attribute("username", "username");

        Response response = Mockito.mock(Response.class);
        controller.transact(request, response); // TODO check return value
    }

    @Property(trials = 1)
    public void testTransactInvalid() throws Exception {
        // TODO figure out how to test (since this talks with the crypto-currency node)
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
                GetUTXWithKeysResponse response = GetUTXWithKeysResponse.failure();
                connectionThread.send(new OutgoingMessage(Message.UTX_WITH_KEYS,
                        ByteUtil.asByteArray(response::serialize))
                );
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Request request = new MockRequest()
                .addQueryParam("recipient", "username")
                .addQueryParam("amount", "100")
                .get();
        request.session().attribute("username", "username");

        Response response = Mockito.mock(Response.class);
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

        Response response = Mockito.mock(Response.class);
        String resp = controller.sendTransaction(request, response); // TODO check return value
        Assert.assertEquals("ok", resp); // TODO this is temporary
    }
}
