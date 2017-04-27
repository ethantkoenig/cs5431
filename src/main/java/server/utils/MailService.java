package server.utils;

public interface MailService {
    void sendEmail(String to, String subject, String body) throws Exception;
}
