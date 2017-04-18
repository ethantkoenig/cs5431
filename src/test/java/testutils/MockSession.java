package testutils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import spark.Session;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class MockSession {
    private final Session session = Mockito.mock(Session.class);
    private final Map<String, Object> attributes = new HashMap<>();

    public MockSession() {
        when(session.attribute(any())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String attribute = invocationOnMock.getArgument(0);
                return attributes.get(attribute);
            }
        });

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                String attribute = invocationOnMock.getArgument(0);
                Object value = invocationOnMock.getArgument(1);
                attributes.put(attribute, value);
                return null;
            }
        }).when(session).attribute(any(), any());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String attribute = invocationOnMock.getArgument(0);
                attributes.remove(attribute);
                return null;
            }
        }).when(session).removeAttribute(any());
    }


    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Session get() {
        return session;
    }

}
