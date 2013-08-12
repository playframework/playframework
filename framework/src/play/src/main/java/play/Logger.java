package play;

/**
 * High level API for logging operations.
 *
 * Example, logging with the default application logger:
 * <pre>
 * Logger.info("Hello!");</pre>
 *
 * Example, logging with a custom logger:
 * <pre>
 * Logger.of("my.logger").info("Hello!")</pre>
 *
 * Each of the logging methods is overloaded to be able to take an array of arguments.  These are formatted into the
 * message String, replacing occurrences of '{}'.  For example:
 *
 * <pre>
 * Logger.info("A {} request was received at {}", request.method(), request.uri());
 * </pre>
 *
 * This might print out:
 *
 * <pre>
 * A POST request was received at /api/items
 * </pre>
 *
 * This saves on the cost of String construction when logging is turned off.
 *
 * This API is intended as a simple logging API to meet 99% percent of the most common logging needs with minimal code
 * overhead.  For more complex needs, the underlying() methods may be used to get the underlying SLF4J logger, or
 * SLF4J may be used directly.
 */
public class Logger {

    private static final ALogger logger = of("application");

    /**
     * Obtain a logger instance.
     *
     * @param name name of the logger
     * @return a logger
     */
    public static ALogger of(String name) {
        return new ALogger(play.api.Logger.apply(name));
    }

    /**
     * Obtain a logger instance.
     *
     * @param clazz a class whose name will be used as logger name
     * @return a logger
     */
    public static ALogger of(Class<?> clazz) {
        return new ALogger(play.api.Logger.apply(clazz));
    }

    /**
     * Get the underlying application SLF4J logger.
     */
    public static org.slf4j.Logger underlying() {
        return logger.underlying();
    }

    /**
     * Returns <code>true</code> if the logger instance enabled for the TRACE level?
     */
    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Returns <code>true</code> if the logger instance enabled for the DEBUG level?
     */
    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Returns <code>true</code> if the logger instance enabled for the INFO level?
     */
    public static boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Returns <code>true</code> if the logger instance enabled for the WARN level?
     */
    public static boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Returns <code>true</code> if the logger instance enabled for the ERROR level?
     */
    public static boolean isErrorEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Log a message with the TRACE level.
     *
     * @param message message to log
     */
    public static void trace(String message) {
        logger.trace(message);
    }

    /**
     * Log a message with the TRACE level.
     *
     * @param message message to log
     * @param args The arguments to apply to the message String
     */
    public static void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    /**
     * Log a message with the TRACE level.
     *
     * @param message message to log
     * @param error associated exception
     */
    public static void trace(String message, Throwable error) {
        logger.trace(message, error);
    }

    /**
     * Log a message with the DEBUG level.
     *
     * @param message message to log
     */
    public static void debug(String message) {
        logger.debug(message);
    }

    /**
     * Log a message with the DEBUG level.
     *
     * @param message message to log
     * @param args The arguments to apply to the message String
     */
    public static void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Log a message with the DEBUG level.
     *
     * @param message message to log
     * @param error associated exception
     */
    public static void debug(String message, Throwable error) {
        logger.debug(message, error);
    }

    /**
     * Log a message with the INFO level.
     *
     * @param message message to log
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Log a message with the INFO level.
     *
     * @param message message to log
     * @param args The arguments to apply to the message string
     */
    public static void info(String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * Log a message with the INFO level.
     *
     * @param message message to log
     * @param error associated exception
     */
    public static void info(String message, Throwable error) {
        logger.info(message, error);
    }

    /**
     * Log a message with the WARN level.
     *
     * @param message message to log
     */
    public static void warn(String message) {
        logger.warn(message);
    }

    /**
     * Log a message with the WARN level.
     *
     * @param message message to log
     * @param args The arguments to apply to the message string
     */
    public static void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * Log a message with the WARN level.
     *
     * @param message message to log
     * @param error associated exception
     */
    public static void warn(String message, Throwable error) {
        logger.warn(message, error);
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param message message to log
     */
    public static void error(String message) {
        logger.error(message);
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param message message to log
     * @param args The arguments to apply to the message string
     */
    public static void error(String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param message message to log
     * @param error associated exception
     */
    public static void error(String message, Throwable error) {
        logger.error(message, error);
    }

    /**
     * Typical logger interface
     */
    public static class ALogger {

        private final play.api.Logger logger;

        public ALogger(play.api.Logger logger) {
            this.logger = logger;
        }

        /**
         * Get the underlying SLF4J logger.
         */
        public org.slf4j.Logger underlying() {
            return logger.underlyingLogger();
        }

        /**
         * Returns <code>true</code> if the logger instance has TRACE level logging enabled.
         */
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        /**
         * Returns <code>true</code> if the logger instance has DEBUG level logging enabled.
         */
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        /**
         * Returns <code>true</code> if the logger instance has INFO level logging enabled.
         */
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        /**
         * Returns <code>true</code> if the logger instance has WARN level logging enabled.
         */
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        /**
         * Returns <code>true</code> if the logger instance has ERROR level logging enabled.
         */
        public boolean isErrorEnabled() {
            return logger.isWarnEnabled();
        }

        /**
         * Logs a message with the TRACE level.
         *
         * @param message message to log
         */
        public void trace(String message) {
            logger.underlyingLogger().trace(message);
        }

        /**
         * Logs a message with the TRACE level.
         *
         * @param message message to log
         * @param args The arguments to apply to the message string
         */
        public void trace(String message, Object... args) {
            logger.underlyingLogger().trace(message, args);
        }

        /**
         * Logs a message with the TRACE level, with the given error.
         *
         * @param message message to log
         * @param error associated exception
         */
        public void trace(String message, Throwable error) {
            logger.underlyingLogger().trace(message, error);
        }

        /**
         * Logs a message with the DEBUG level.
         *
         * @param message Message to log
         */
        public void debug(String message) {
            logger.underlyingLogger().debug(message);
        }

        /**
         * Logs a message with the DEBUG level.
         *
         * @param message Message to log
         * @param args The arguments to apply to the message string
         */
        public void debug(String message, Object... args) {
            logger.underlyingLogger().debug(message, args);
        }

        /**
         * Logs a message with the DEBUG level, with the given error.
         *
         * @param message Message to log
         * @param error associated exception
         */
        public void debug(String message, Throwable error) {
            logger.underlyingLogger().debug(message, error);
        }

        /**
         * Logs a message with the INFO level.
         *
         * @param message message to log
         */
        public void info(String message) {
            logger.underlyingLogger().info(message);
        }

        /**
         * Logs a message with the INFO level.
         *
         * @param message message to log
         * @param args The arguments to apply to the message string
         */
        public void info(String message, Object... args) {
            logger.underlyingLogger().info(message, args);
        }

        /**
         * Logs a message with the INFO level, with the given error.
         *
         * @param message message to log
         * @param error associated exception
         */
        public void info(String message, Throwable error) {
            logger.underlyingLogger().info(message, error);
        }

        /**
         * Log a message with the WARN level.
         *
         * @param message message to log
         */
        public void warn(String message) {
            logger.underlyingLogger().warn(message);
        }

        /**
         * Log a message with the WARN level.
         *
         * @param message message to log
         * @param args The arguments to apply to the message string
         */
        public void warn(String message, Object... args) {
            logger.underlyingLogger().warn(message, args);
        }

        /**
         * Log a message with the WARN level, with the given error.
         *
         * @param message message to log
         * @param error associated exception
         */
        public void warn(String message, Throwable error) {
            logger.underlyingLogger().warn(message, error);
        }

        /**
         * Log a message with the ERROR level.
         *
         * @param message message to log
         */
        public void error(String message) {
            logger.underlyingLogger().error(message);
        }

        /**
         * Log a message with the ERROR level.
         *
         * @param message message to log
         * @param args The arguments to apply to the message string
         */
        public void error(String message, Object... args) {
            logger.underlyingLogger().error(message, args);
        }

        /**
         * Log a message with the ERROR level, with the given error.
         *
         * @param message message to log
         * @param error associated exception
         */
        public void error(String message, Throwable error) {
            logger.underlyingLogger().error(message, error);
        }

    }

}
