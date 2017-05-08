package testutils;

import com.google.inject.AbstractModule;
import server.utils.ConnectionProvider;
import server.utils.MailService;

import java.security.SecureRandom;

public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SecureRandom.class).to(InsecureSecureRandom.class);
        bind(ConnectionProvider.class).to(TestConnectionProvider.class);
        bind(MailService.class).to(MockMailService.class);
    }
}
