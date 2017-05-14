package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Log {
    private static Log parentLog = getParentLog();

    private static Log getParentLog() {
        return new DefaultLog(Logger.getLogger(""));
    }

    public static Log forClass(Class<?> clazz) {
        return new DefaultLog(Logger.getLogger(clazz.getName()));
    }

    public static Log forClass(Class<?> clazz, String name) {
        String loggerName = String.format("%s(%s)", clazz.getName(), name);
        return new DefaultLog(Logger.getLogger(loggerName));
    }

    public static Log parentLog() {
        return parentLog;
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void warning(String format, Object... args) {
        log(Level.WARNING, format, args);
    }

    public void severe(String format, Object... args) {
        log(Level.SEVERE, format, args);
    }

    public abstract void log(Level level, String format, Object... args);

    public abstract Logger logger();

    private static final class DefaultLog extends Log {
        private final Logger logger;

        private DefaultLog(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void log(Level level, String format, Object... args) {
            if (logger.isLoggable(level)) {
                logger.log(level, String.format(format, args));
            }
        }

        @Override
        public Logger logger() {
            return logger;
        }
    }
}
