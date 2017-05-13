package server.utils;

import message.IncomingMessage;
import message.OutgoingMessage;
import utils.DeserializationException;

import java.io.IOException;

public interface CryptocurrencyEndpoint extends AutoCloseable {
    void send(OutgoingMessage message) throws IOException;

    IncomingMessage receive() throws DeserializationException, IOException;

    interface Provider {
        CryptocurrencyEndpoint getEndpoint() throws IOException;
    }
}
