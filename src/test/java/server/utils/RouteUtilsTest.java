package server.utils;

import org.junit.Test;
import spark.Request;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static server.utils.RouteUtils.*;
import static testutils.TestUtils.assertThrows;
import static testutils.TestUtils.bytes;

public class RouteUtilsTest {

    @Test
    public void testQueryParam() throws Exception {
        Request request = mockRequest("param", "value");
        assertEquals("value", queryParam(request, "param"));

        Request nullRequest = mockRequest("param", null);
        assertThrows("",
                () -> queryParam(nullRequest, "param"),
                RouteUtils.InvalidParamException.class);
    }

    @Test
    public void testQueryParamHex() throws Exception {
        Request validRequest = mockRequest("param", "0123ab");
        assertArrayEquals(bytes(0x01, 0x23, 0xab),
                queryParamHex(validRequest, "param"));

        Request invalidRequest = mockRequest("param", "thisIsNotHex!");
        assertThrows("",
                () -> queryParamHex(invalidRequest, "param"),
                RouteUtils.InvalidParamException.class);

        Request nullRequest = mockRequest("param", null);
        assertThrows("",
                () -> queryParamHex(nullRequest, "param"),
                RouteUtils.InvalidParamException.class);
    }

    @Test
    public void testQueryParamInt() throws Exception {
        Request validRequest = mockRequest("param", "12345");
        assertEquals(12345, queryParamInt(validRequest, "param"));

        Request invalidRequest = mockRequest("param", "notANumber!");
        assertThrows("",
                () -> queryParamInt(invalidRequest, "param"),
                RouteUtils.InvalidParamException.class);

        Request nullRequest = mockRequest("param", null);
        assertThrows("",
                () -> queryParamInt(nullRequest, "param"),
                RouteUtils.InvalidParamException.class);
    }

    @Test
    public void testQueryParamLong() throws Exception {
        Request validRequest = mockRequest("param", "1099511627776");
        assertEquals(1099511627776L,
                queryParamLong(validRequest, "param"));

        Request invalidRequest = mockRequest("param", "notANumber!");
        assertThrows("", () ->
                        queryParamLong(invalidRequest, "param"),
                RouteUtils.InvalidParamException.class);

        Request nullRequest = mockRequest("param", null);
        assertThrows("",
                () -> queryParamLong(nullRequest, "param"),
                RouteUtils.InvalidParamException.class);
    }

    private Request mockRequest(String param, String value) {
        Request request = mock(Request.class);
        when(request.queryParams(param)).thenReturn(value);
        return request;
    }
}
