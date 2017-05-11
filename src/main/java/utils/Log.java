package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
    private static Log parentLog = forClass(Log.class);
    public final Logger logger;

    private Log(Logger logger) {
        this.logger = logger;
    }

    public static Log forClass(Class<?> clazz) {
        String name = Log.class.getName() + "." + clazz.getName();
        return new Log(Logger.getLogger(name));
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

    public void log(Level level, String format, Object... args) {
        if (logger.isLoggable(level)) {
            logger.log(level, String.format(format, args));
        }
    }

}
