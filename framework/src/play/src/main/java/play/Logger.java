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
