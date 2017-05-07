package testutils;

import com.google.gson.Gson;
import org.mockito.Mockito;
import spark.Request;
import spark.Session;
import utils.ByteUtil;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

public class MockRequest {
    private final Request mock = Mockito.mock(Request.class);
    private final MockSession mockSession = new MockSession();

    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private String body = null;

    public MockRequest() {
        when(mock.params(any())).then(invocationOnMock -> {
            String param = invocationOnMock.getArgument(0);
            return params.get(param);
        });

        when(mock.queryParams(any())).then(invocationOnMock -> {
            String param = invocationOnMock.getArgument(0);
            return queryParams.get(param);
        });

        when(mock.body()).then(invocationOnMock -> body);

        when(mock.queryParams()).then(invocationOnMock -> queryParams.keySet());

        when(mock.url()).thenReturn("https://localhost:5000/foo/bar");
    }

    public MockRequest jsonBody(Object object) {
        body = new Gson().toJson(object);
        return this;
    }

    public MockRequest setBody(String body) {
        this.body = body;
        return this;
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
