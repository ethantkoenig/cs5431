package server.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import server.access.UserAccess;
import server.models.User;
import spark.*;
import utils.ByteUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

public final class RouteUtils {
    private final UserAccess userAccess;
    private final Gson gson;

    @Inject
    public RouteUtils(UserAccess userAccess, Gson gson) {
        this.userAccess = userAccess;
        this.gson = gson;
    }

    public TemplateViewRoute template(String templatePath) {
        return (request, response) -> modelAndView(request, templatePath).get();
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
            String msg = String.format("Parameter %s expected, but not found", paramName);
            throw new InvalidParamException(msg);
        }
        return value;
    }

    public static byte[] queryParamHex(Request request, String paramName)
            throws InvalidParamException {
        String param = queryParam(request, paramName);
        return ByteUtil.hexStringToByteArray(param).orElseThrow(() -> {
            String msg = String.format("Expected hexadecimal value for parameter %s, found %s",
                    paramName, param);
            return new InvalidParamException(msg);
        });
    }

    public static int queryParamInt(Request request, String paramName)
            throws InvalidParamException {
        String param = queryParam(request, paramName);
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            String msg = String.format(
                    "Expected long value for parameter %s, found %s",
                    paramName, param
            );
            throw new InvalidParamException(msg);
        }
    }

    public static long queryParamLong(Request request, String paramName)
            throws InvalidParamException {
        String param = queryParam(request, paramName);
        try {
            return Long.parseLong(param);
        } catch (NumberFormatException e) {
            String msg = String.format(
                    "Expected long value for parameter %s, found %s",
                    paramName, param
            );
            throw new InvalidParamException(msg);
        }
    }

    public static String baseURL(Request request) throws IOException {
        URL url = new URL(request.url());
        return String.format("%s://%s", url.getProtocol(), url.getAuthority());
    }

    public <T> T parseBody(Request request, Class<T> clazz)
            throws InvalidParamException {
        try {
            T t = gson.fromJson(request.body(), clazz);
            if (t == null) {
                throw new InvalidParamException("Invalid request body: empty");
            }
            return t;
        } catch (JsonSyntaxException e) {
            String msg = String.format("Invalid request body: %s", e.getMessage());
            throw new InvalidParamException(msg);
        }
    }

    public String toJson(Response response, Object o) {
        response.type("application/json");
        return gson.toJson(o);
    }

    public static class NotLoggedInException extends Exception {
    }

    public static class InvalidParamException extends Exception {
        public InvalidParamException(String message) {
            super(message);
        }
    }
}
