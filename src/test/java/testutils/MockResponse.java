package testutils;

import org.mockito.Mockito;
import spark.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

public class MockResponse {
    private final Response mock = Mockito.mock(Response.class);

    private String redirectedTo = null;
    private int status = 0;

    public MockResponse() {
        doAnswer(invocationOnMock -> {
            redirectedTo = invocationOnMock.getArgument(0);
            return null;
        }).when(mock).redirect(any());

        doAnswer(invocationOnMock -> {
            status = invocationOnMock.getArgument(0);
            return null;
        }).when(mock).status(anyInt());
    }

    public boolean redirected() {
        return redirectedTo != null;
    }

    public String redirectedTo() {
        return redirectedTo;
    }

    public int status() {
        return status;
    }

    public Response get() {
        return mock;
    }
}
