package testutils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import spark.Request;
import spark.Session;
import utils.ByteUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

public class MockRequest {
    private final Request mock = Mockito.mock(Request.class);
    private final MockSession mockSession = new MockSession();

    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();

    public MockRequest() {
        when(mock.params(any())).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                String param = invocationOnMock.getArgument(0);
                return params.get(param);
            }
        });

        when(mock.queryParams(any())).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                String param = invocationOnMock.getArgument(0);
                return queryParams.get(param);
            }
        });

        when(mock.queryParams()).then(new Answer<Set<String>>() {
            @Override
            public Set<String> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return queryParams.keySet();
            }
        });
    }

    public MockRequest addParam(String param, String value) {
        params.put(param, value);
        return this;
    }

    public MockRequest addQueryParam(String param, String value) {
        queryParams.put(param, value);
        return this;
    }

    public MockRequest addQueryParamHex(String param, byte[] bytes) {
        return addQueryParam(param, ByteUtil.bytesToHexString(bytes));
    }

    public MockRequest addSessionAttribute(String name, Object value) {
        mockSession.addAttribute(name, value);
        return this;
    }

    public Request get() {
        Session session = mockSession.get();
        when(mock.session()).thenReturn(session);
        when(mock.session(anyBoolean())).thenReturn(session);
        return mock;
    }
}
