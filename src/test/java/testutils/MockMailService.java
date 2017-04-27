package testutils;

import server.utils.MailService;

public final class MockMailService implements MailService {
    @Override
    public void sendEmail(String to, String subject, String body) {
        // no-op
    }
}
