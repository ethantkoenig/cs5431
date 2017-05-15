package server.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import utils.ByteUtil;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GmailService implements MailService {
    private static final String APPLICATION_NAME = "EzraCoinL Wallet Email Service";
    private static final File DATA_STORE_DIR = new File("gmail-data-store");
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String EMAIL_ADDRESS = "ezracoinl@gmail.com";

    private final HttpTransport httpTransport;
    private final FileDataStoreFactory dataStoreFactory;
    private final Gmail service;

    public GmailService() throws Exception {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        service = getGmailService();
    }

    private Credential authorize() throws IOException {
        try (InputStream in = GmailService.class.getResourceAsStream("/client_secret.json")) {
            Reader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, inReader);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES
            ).setDataStoreFactory(dataStoreFactory).build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize("user");
        }
    }

    private Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private MimeMessage constructMessage(String to, String subject, String body)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(EMAIL_ADDRESS));
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO,
                InternetAddress.parse(to));
        mimeMessage.setSubject(subject);
        mimeMessage.setText(body);
        return mimeMessage;
    }

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        MimeMessage mimeMessage = constructMessage(to, subject, body);
        byte[] bytes = ByteUtil.asByteArray(mimeMessage::writeTo);
        String encoded = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encoded);
        service.users().messages().send("me", message).execute();
    }
}
