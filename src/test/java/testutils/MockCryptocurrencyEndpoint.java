package testutils;

import com.google.inject.Singleton;
import message.IncomingMessage;
import message.OutgoingMessage;
import org.junit.Assert;
import server.utils.CryptocurrencyEndpoint;
import utils.DeserializationException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Function;

public class MockCryptocurrencyEndpoint implements CryptocurrencyEndpoint {
    private final Queue<OutgoingMessage> sentMessages = new ArrayDeque<>();
    private final Queue<IncomingMessage> responses = new ArrayDeque<>();

    private Function<OutgoingMessage, OutgoingMessage> respond;

    @Override
    public void send(OutgoingMessage message) throws IOException {
        sentMessages.add(message);
        if (respond != null) {
            OutgoingMessage response = respond.apply(message);
            responses.add(new IncomingMessage(response.type, response.payload));
        }
    }

    @Override
    public IncomingMessage receive() throws DeserializationException, IOException {
        if (responses.isEmpty()) {
            Assert.fail("Responses queue is empty");
        }
        return responses.remove();
    }

    @Override
    public void close() throws Exception {
        // no-op
    }

    public void respondWith(Function<OutgoingMessage, OutgoingMessage> respond) {
        this.respond = respond;
    }

    public OutgoingMessage sent() {
        if (sentMessages.isEmpty()) {
            Assert.fail("Sent messages queue is empty");
        }
        return sentMessages.remove();
    }

    public void clear() {
        sentMessages.clear();
        responses.clear();
    }

    @Singleton
    public static final class Provider implements CryptocurrencyEndpoint.Provider {
        private final MockCryptocurrencyEndpoint endpoint = new MockCryptocurrencyEndpoint();

        @Override
        public MockCryptocurrencyEndpoint getEndpoint() throws IOException {
            return endpoint;
        }
    }
}
