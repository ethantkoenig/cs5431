package server.utils;

import server.access.UserAccess;
import server.models.User;
import spark.Request;
import spark.Route;
import utils.ByteUtil;

import java.sql.SQLException;

public class RouteUtils {

    public static Route wrapRoute(Route route) {
        return (request, response) -> {
            try {
                return route.handle(request, response);
            } catch (NotLoggedInException e) {
                response.status(403);
                return "";
            } catch (InvalidParamException e) {
                response.status(400);
                return "";
            }
        };
    }

    public static User loggedInUser(Request request)
            throws NotLoggedInException, SQLException {
        String username = request.session().attribute("username");
        if (username == null) {
            throw new NotLoggedInException();
        }
        User user = UserAccess.getUserbyUsername(username);
        if (user == null) {
            throw new NotLoggedInException();
        }
        return user;
    }

    public static String queryParam(Request request, String paramName)
            throws InvalidParamException {
        String value = request.queryParams(paramName);
        if (value == null) {
            throw new InvalidParamException();
        }
        return value;
    }

    public static byte[] queryParamHex(Request request, String paramName)
            throws InvalidParamException {
        return ByteUtil.hexStringToByteArray(queryParam(request, paramName))
                .orElseThrow(InvalidParamException::new);
    }

    public static int queryParamInt(Request request, String paramName)
            throws InvalidParamException {
        try {
            return Integer.parseInt(queryParam(request, paramName));
        } catch (NumberFormatException e) {
            throw new InvalidParamException();
        }
    }

    public static long queryParamLong(Request request, String paramName)
            throws InvalidParamException {
        try {
            return Long.parseLong(queryParam(request, paramName));
        } catch (NumberFormatException e) {
            throw new InvalidParamException();
        }
    }

    public static class NotLoggedInException extends Exception {
    }

    public static class InvalidParamException extends Exception {
    }
}
