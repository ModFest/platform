package net.modfest.platform.log;

import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;

public class ModFestLog {
    private static final Logger LOGGER = Loggers.getLogger("ModFest");

    private static final LogFile LIFECYCLE_LOG = new LogFile("lifecycle");
    private static final LogFile ERROR_LOG = new LogFile("error");
    private static final LogFile INFO_LOG = new LogFile("info");
    private static final LogFile DEBUG_LOG = new LogFile("debug");
    private static final LogFile ALL_LOG = new LogFile("all");

    public static void lifecycle(String message, Object... arguments) {
        LOGGER.info(message, arguments);
        LIFECYCLE_LOG.log(message, arguments);
        ALL_LOG.log(message, arguments);
    }

    public static void error(String message, Object... arguments) {
        LOGGER.error(message, arguments);
        ERROR_LOG.log(message, arguments);
        ALL_LOG.log(message, arguments);
    }

    public static void info(String message, Object... arguments) {
        LOGGER.info(message, arguments);
        INFO_LOG.log(message, arguments);
        ALL_LOG.log(message, arguments);
    }

    public static void debug(String message, Object... arguments) {
        LOGGER.debug(message, arguments);
        DEBUG_LOG.log(message, arguments);
        ALL_LOG.log(message, arguments);
    }

    public static void close() {
        try {
            DEBUG_LOG.close();
            INFO_LOG.close();
            LIFECYCLE_LOG.close();
            ERROR_LOG.close();
            ALL_LOG.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
