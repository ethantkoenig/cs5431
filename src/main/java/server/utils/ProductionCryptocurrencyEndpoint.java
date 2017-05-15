package server.utils;

import com.google.inject.Inject;
import message.IncomingMessage;
import message.OutgoingMessage;
import network.Connection;
import server.annotations.NodeAddress;
import utils.DeserializationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ProductionCryptocurrencyEndpoint implements CryptocurrencyEndpoint {
    private final Connection connection;

    ProductionCryptocurrencyEndpoint(InetSocketAddress address) throws IOException {
        Socket socket = new Socket(address.getAddress(), address.getPort());
        this.connection = Connection.connect(socket, false);
    }

    @Override
    public void send(OutgoingMessage message) throws IOException {
        connection.send(message);
    }

    @Override
    public IncomingMessage receive() throws DeserializationException, IOException {
        return connection.receive();
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    public static final class Provider implements CryptocurrencyEndpoint.Provider {
        private final InetSocketAddress address;

        @Inject
        public Provider(@NodeAddress InetSocketAddress address) {
            this.address = address;
        }

        @Override
        public CryptocurrencyEndpoint getEndpoint() throws IOException {
            return new ProductionCryptocurrencyEndpoint(address);
        }
    }
}
