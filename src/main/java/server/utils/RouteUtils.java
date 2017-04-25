package server.utils;

import com.google.inject.Inject;
import server.access.UserAccess;
import server.models.User;
import spark.*;
import utils.ByteUtil;

import java.sql.SQLException;
import java.util.HashMap;
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
                return RouteUtils.redirectTo(response, "/login");
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
        Session session = request.session();
        MapModelAndView mapModelAndView = new MapModelAndView(viewName)
                .add("loggedIn", loggedIn)
                .add("loggedInUsername", username)
                .addIfNonNull("error", session.attribute("error"))
                .addIfNonNull("alert", session.attribute("alert"))
                .addIfNonNull("success", session.attribute("success"));
        session.removeAttribute("error");
        session.removeAttribute("alert");
        session.removeAttribute("success");
        return mapModelAndView;
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

    public static void successMessage(Request request, String message) {
        request.session().attribute("success", message);
    }

    public static void alertMessage(Request request, String message) {
        request.session().attribute("alert", message);
    }

    public static void errorMessage(Request request, String message) {
        request.session().attribute("error", message);
    }

    public static ModelAndView redirectTo(Response response, String path) {
        response.redirect(path);
        // return whatever, will be overridden by the redirect
        return new ModelAndView(new HashMap<String, Object>(), "");
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
