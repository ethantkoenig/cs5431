package server.utils;

import utils.Config;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

public class GmailService implements MailService {

    private static final String SUBJECT = "Yaccoin Password Recovery";
    private static final String BODY = "Please click on the provided link in order to create a new account password.";
    private static final Logger LOGGER = Logger.getLogger(GmailService.class.getName());

    @Override
    public void sendEmail(String to, String link) {
        Properties props = setGmailProp();
        Session session = Session.getInstance(props, new GmailAuthenticator());

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Config.getMailFrom()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject(SUBJECT);
            message.setText(BODY + "\n" + link);

            Transport.send(message);
        } catch (MessagingException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    private static Properties setGmailProp() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587"); //465 for SSL
        return properties;
    }

    private static final class GmailAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(Config.getMailFrom(), Config.getMailPassword());
        }
    }
}
