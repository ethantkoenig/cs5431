package server.utils;

import com.google.inject.Inject;
import server.access.UserAccess;
import server.models.User;
import spark.Request;
import spark.Route;
import spark.TemplateViewRoute;
import utils.ByteUtil;

import java.sql.SQLException;
import java.util.Optional;

public final class RouteUtils {

    private final UserAccess userAccess;

    @Inject
    public RouteUtils(UserAccess userAccess) {
        this.userAccess = userAccess;
    }

    public static Route wrapRoute(Route route) {
        return (request, response) -> {
            try {
                return route.handle(request, response);
            } catch (NotLoggedInException e) {
                response.status(403);
                return "";
            } catch (InvalidParamException e) {
                response.status(400);
                response.body("Invalid Parameters.");
                return "";
            }
        };
    }

    public static TemplateViewRoute wrapTemplate(TemplateViewRoute route) {
        return (request, response) -> {
            try {
                return route.handle(request, response);
            } catch (NotLoggedInException e) {
                response.redirect("/login");
                return null;
            } catch (InvalidParamException e) {
                // TODO find better way to handle
                response.status(400);
                response.body("Invalid Parameters.");
                return null;
            }
        };
    }

    public TemplateViewRoute template(String templatePath) {
        return wrapTemplate((request, response) -> modelAndView(request, templatePath).get());
    }

    public MapModelAndView modelAndView(Request request, String viewName)
            throws SQLException {
        Optional<User> optUser = loggedInUser(request);
        boolean loggedIn = optUser.isPresent();
        String username = optUser.map(User::getUsername).orElse("");
        return new MapModelAndView(viewName)
                .add("loggedIn", loggedIn)
                .add("loggedInUsername", username);
    }

    public Optional<User> loggedInUser(Request request)
            throws SQLException {
        String username = request.session().attribute("username");
        if (username == null) {
            return Optional.empty();
        }
        return userAccess.getUserByUsername(username);
    }

    public User forceLoggedInUser(Request request)
            throws NotLoggedInException, SQLException {
        return loggedInUser(request).orElseThrow(NotLoggedInException::new);
    }

    public static boolean queryParamExists(Request request, String paramName) {
        return request.queryParams().contains(paramName);
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
