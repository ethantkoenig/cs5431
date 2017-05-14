package server.controllers;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import crypto.ECDSAPublicKey;
import message.Message;
import message.OutgoingMessage;
import message.payloads.GetUTXWithKeysResponsePayload;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import server.access.TransactionAccess;
import server.bodies.SendTransactionBody;
import server.bodies.SignatureBody;
import server.bodies.TransactionResponseBody;
import server.models.Key;
import server.models.User;
import server.utils.ConnectionProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import testutils.*;
import transaction.Transaction;
import utils.ByteUtil;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static testutils.TestUtils.assertPresent;

@RunWith(JUnitQuickcheck.class)
public class TransactionControllerTest extends ControllerTest {
    private TransactionController controller;
    private TransactionAccess access;
    private Fixtures fixtures;
    private MockCryptocurrencyEndpoint endpoint;

    public TransactionControllerTest() throws Exception {
        super();
        Injector injector = Guice.createInjector(new TestModule());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
        access = injector.getInstance(TransactionAccess.class);
        setConnectionProvider(injector.getInstance(ConnectionProvider.class));
        fixtures = injector.getInstance(Fixtures.class);
        endpoint = injector.getInstance(MockCryptocurrencyEndpoint.Provider.class).getEndpoint();
    }

    @Before
    public void clearEndpoint() {
        endpoint.clear();
    }

    @Test
    public void testGetTransact() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = template(controller::getTransact).handle(request, mockResponse.get());
        assertEquals("transact.ftl", modelAndView.getViewName());
    }

    @Property(trials = 1)
    public void testTransactValid(Transaction transaction) throws Exception {
        final int senderId = 1;
        final int recipientId = 2;

        final Key key = fixtures.keyOwnedBy(senderId);
        ECDSAPublicKey ecKey = assertPresent(key.asKey());
        OutgoingMessage response = GetUTXWithKeysResponsePayload.success(
                Collections.singletonList(ecKey),
                transaction
        ).toMessage();
        endpoint.respondWith(message -> {
            assertEquals(Message.GET_UTX_WITH_KEYS, message.type);
            return response;
        });

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user(recipientId).getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "test message")
                .addSessionAttribute("username", fixtures.user(senderId).getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        Object json = route(controller::transact).handle(request, mockResponse.get());
        TransactionResponseBody body = new Gson().fromJson(json.toString(), TransactionResponseBody.class);
        assertEquals(1, body.encryptedKeys.size());
        assertEquals(key.encryptedPrivateKey, body.encryptedKeys.get(0));
    }

    @Test
    public void testTransactInvalid() throws Exception {
        OutgoingMessage response = GetUTXWithKeysResponsePayload.failure().toMessage();
        endpoint.respondWith(sent -> {
            assertEquals(Message.GET_UTX_WITH_KEYS, sent.type);
            return response;
        });

        Request request = new MockRequest()
                .addQueryParam("recipient", fixtures.user(1).getUsername())
                .addQueryParam("amount", "100")
                .addQueryParam("message", "this is a test message")
                .addSessionAttribute("username", fixtures.user(2).getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::transact).handle(request, mockResponse.get());
    }

    @Property(trials = 1)
    public void testSendTransaction(Transaction transaction) throws Exception {
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
        route(controller::sendTransaction).handle(request, response);

        OutgoingMessage sent = endpoint.sent();
        assertEquals(Message.TRANSACTION, sent.type);
        assertArrayEquals(ByteUtil.asByteArray(transaction::serialize), sent.payload);
    }

    @Test
    public void testSendTransactionInvalidBody() throws Exception {
        Request request = new MockRequest()
                .setBody(randomAsciiString(1024))
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();

        MockResponse mockResponse = new MockResponse();
        route(controller::sendTransaction).handle(request, mockResponse.get());
        Assert.assertEquals(400, mockResponse.status());
    }

    @Test
    public void testGetRequests() throws Exception {
        Request request = new MockRequest()
                .addSessionAttribute("username", fixtures.user(1).getUsername())
                .get();
        MockResponse mockResponse = new MockResponse();
        ModelAndView modelAndView = template(controller::getRequests).handle(request, mockResponse.get());
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
        route(controller::createRequest).handle(request, mockResponse.get());
        boolean requestAdded = access.getRequests(sender).stream()
                .anyMatch(t -> t.isRequest()
                        && t.getToUser().equals(recipient)
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
                                && t.getToUser().equals(recipient.getUsername())
                                && t.getMessage().equals(message)
                                && t.getAmount() == amount)
                        .findFirst()
        );
        Request request = new MockRequest()
                .addSessionAttribute("username", sender.getUsername())
                .addQueryParam("tranid", Integer.toString(transaction.getTranId()))
                .get();
        MockResponse mockResponse = new MockResponse();
        route(controller::deleteRequest).handle(request, mockResponse.get());

        Assert.assertFalse(access.getRequests(sender.getUsername()).stream()
                .anyMatch(t -> t.getTranId() == transaction.getTranId())
        );
    }
}
