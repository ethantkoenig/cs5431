package testutils;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import spark.Request;
import spark.Session;
import utils.ByteUtil;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class MockRequest {
    private Request mock = Mockito.mock(Request.class);
    private Session session = Mockito.mock(Session.class);

    public MockRequest() {
        when(mock.session()).thenReturn(session);
        when(mock.session(anyBoolean())).thenReturn(session);

        ArgumentCaptor<String> nameArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueArg = ArgumentCaptor.forClass(Object.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                when(session.attribute(nameArg.getValue())).thenReturn(valueArg.getValue());
                return null;
            }
        }).when(session).attribute(nameArg.capture(), valueArg.capture());
    }

    public MockRequest addParam(String param, String value) {
        when(mock.params(param)).thenReturn(value);
        return this;
    }

    public MockRequest addQueryParam(String param, String value) {
        when(mock.queryParams(param)).thenReturn(value);
        return this;
    }

    public MockRequest addQueryParamHex(String param, byte[] bytes) {
        return addQueryParam(param, ByteUtil.bytesToHexString(bytes));
    }

    public MockRequest addSessionAttribute(String name, Object value) {
        when(session.attribute(name)).thenReturn(value);
        return this;
    }

    public Request get() {
        return mock;
    }
}
