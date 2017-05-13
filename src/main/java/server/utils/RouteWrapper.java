package server.utils;

import spark.*;
import utils.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class RouteWrapper {
    private final Log log;

    public RouteWrapper(Log log) {
        this.log = log;
    }

    @FunctionalInterface
    public interface LoggedRoute {
        Object handle(Request request, Response response, Log log) throws Exception;
    }

    @FunctionalInterface
    public interface LoggedTemplateViewRoute {
        ModelAndView handle(Request request, Response response, Log log) throws Exception;
    }

    public Route route(LoggedRoute route) {
        return (request, response) -> {
            try {
                return route.handle(request, response, getLog(request));
            } catch (RouteUtils.NotLoggedInException e) {
                response.status(403);
                return "";
            } catch (RouteUtils.InvalidParamException e) {
                response.status(400);
                return e.getMessage();
            }
        };
    }

    public TemplateViewRoute template(LoggedTemplateViewRoute route) {
        return (request, response) -> {
            try {
                return route.handle(request, response, getLog(request));
            } catch (RouteUtils.NotLoggedInException e) {
                return RouteUtils.redirectTo(response, "/login");
            } catch (RouteUtils.InvalidParamException e) {
                response.status(400);
                response.body("Invalid Parameters.");
                return null;
            }
        };
    }

    private Log getLog(Request request) {
        return new RouteLog(log.logger(), request);
    }

    private static final class RouteLog extends Log {
        public final Logger logger;
        public final Request request;

        private RouteLog(Logger logger, Request request) {
            this.logger = logger;
            this.request = request;
        }

        @Override
        public void log(Level level, String format, Object... args) {
            if (!logger.isLoggable(level)) {
                return;
            }
            String header = String.format("[session=%s] ", request.session().id());
            String body = String.format(format, args);
            logger.log(level, header + body);
        }

        @Override
        public Logger logger() {
            return logger;
        }
    }
}
