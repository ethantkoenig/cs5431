package testutils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import spark.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class MockResponse {
    private final Response mock = Mockito.mock(Response.class);

    private String redirectedTo = null;

    public MockResponse() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                redirectedTo = invocationOnMock.getArgument(0);
                return null;
            }
        }).when(mock).redirect(any());
    }

    public boolean redirected() {
        return redirectedTo != null;
    }

    public String redirectedTo() {
        return redirectedTo;
    }

    public Response get() {
        return mock;
    }
}
