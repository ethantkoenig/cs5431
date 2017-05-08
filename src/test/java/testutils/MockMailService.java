package testutils;

import com.google.inject.Singleton;
import org.junit.Assert;
import server.utils.MailService;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Singleton
public final class MockMailService implements MailService {

    private final Map<String, String> sentMail = new HashMap<>();

    @Override
    public void sendEmail(String to, String subject, String body) {
        sentMail.put(to, body);
    }

    public boolean sentTo(String to) {
        return sentMail.containsKey(to);
    }

    public String assertMailToGetBody(String to) {
        Assert.assertTrue(
                String.format("No mail sent to %s", to),
                sentMail.containsKey(to)
        );
        return sentMail.get(to);
    }

    public static URL getURL(String body) throws IOException {
        String[] words = body.split("\\s+");
        for (String word : words) {
            if (word.startsWith("https://")) {
                return new URL(word);
            }
        }
        Assert.fail(String.format("No URL in the body [%s]", body));
        return null;
    }

    public static String getParam(URL url, String paramName) {
        String queryString = url.getQuery();
        for (String param : queryString.split("&")) {
            param = param.trim();
            if (!param.contains("=")) {
                continue;
            }
            int index = param.indexOf("=");
            if (param.substring(0, index).equals(paramName)) {
                return param.substring(index + 1);
            }
        }
        Assert.fail(
                String.format("No such param %s in query string %s", paramName, queryString)
        );
        return null;
    }
}
