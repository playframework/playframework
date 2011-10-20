package play;

/**
 * High level API for logging operations.
 *
 * Example, logging with the default application logger:
 * {{{
 * Logger.info("Hello!")
 * }}}
 *
 * Example, logging with a custom logger:
 * {{{
 * Logger.of("my.logger").info("Hello!")
 * }}}
 *
 */
public class Logger {
    
    private static final ALogger logger = of("application");
    
    /**
     * Obtain a logger instance.
     *
     * @param name Name of the logger.
     * @return A logger.
     */
    public static ALogger of(String name) {
        return new ALogger(play.api.Logger.apply(name));
    }
    
    /**
     * Obtain a logger instance.
     *
     * @param clazz A class whose name will be used as logger name.
     * @return A logger.
     */
    public static ALogger of(Class<?> clazz) {
        return new ALogger(play.api.Logger.apply(clazz));
    }
    
    /**
     * Is the logger instance enabled for the TRACE level?
     */
    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    } 

    /**
     * Is the logger instance enabled for the DEBUG level?
     */
    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Is the logger instance enabled for the INFO level?
     */
    public static boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Is the logger instance enabled for the WARN level?
     */
    public static boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Is the logger instance enabled for the ERROR level?
     */
    public static boolean isErrorEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Log a message with the TRACE level.
     *
     * @param message Message to log.
     */
    public static void trace(String message) { 
        logger.trace(message);
    }

    /**
     * Log a message with the TRACE level.
     *
     * @param message Message to log.
     * @param error Associated exception.
     */
    public static void trace(String message, Throwable error) { 
        logger.trace(message, error);
    }

    /**
     * Log a message with the DEBUG level.
     *
     * @param message Message to log.
     */
    public static void debug(String message) { 
        logger.debug(message);
    }

    /**
     * Log a message with the DEBUG level.
     *
     * @param message Message to log.
     * @param error Associated exception.
     */
    public static void debug(String message, Throwable error) { 
        logger.debug(message, error);
    }

    /**
     * Log a message with the INFO level.
     *
     * @param message Message to log.
     */
    public static void info(String message) { 
        logger.info(message);
    }

    /**
     * Log a message with the INFO level.
     *
     * @param message Message to log.
     * @param error Associated exception.
     */
    public static void info(String message, Throwable error) { 
        logger.info(message, error);
    }

    /**
     * Log a message with the WARN level.
     *
     * @param message Message to log.
     */
    public static void warn(String message) { 
        logger.warn(message);
    }

    /**
     * Log a message with the WARN level.
     *
     * @param message Message to log.
     * @param error Associated exception.
     */
    public static void warn(String message, Throwable error) { 
        logger.warn(message, error);
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param message Message to log.
     */
    public static void error(String message) { 
        logger.error(message);
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param message Message to log.
     * @param error Associated exception.
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
         * Is the logger instance enabled for the TRACE level?
         */
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        } 

        /**
         * Is the logger instance enabled for the DEBUG level?
         */
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        /**
         * Is the logger instance enabled for the INFO level?
         */
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        /**
         * Is the logger instance enabled for the WARN level?
         */
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        /**
         * Is the logger instance enabled for the ERROR level?
         */
        public boolean isErrorEnabled() {
            return logger.isWarnEnabled();
        }

        /**
         * Log a message with the TRACE level.
         *
         * @param message Message to log.
         */
        public void trace(String message) { 
            logger.trace(message);
        }

        /**
         * Log a message with the TRACE level.
         *
         * @param message Message to log.
         * @param error Associated exception.
         */
        public void trace(String message, Throwable error) { 
            logger.trace(message, error);
        }

        /**
         * Log a message with the DEBUG level.
         *
         * @param message Message to log.
         */
        public void debug(String message) { 
            logger.debug(message);
        }

        /**
         * Log a message with the DEBUG level.
         *
         * @param message Message to log.
         * @param error Associated exception.
         */
        public void debug(String message, Throwable error) { 
            logger.debug(message, error);
        }

        /**
         * Log a message with the INFO level.
         *
         * @param message Message to log.
         */
        public void info(String message) { 
            logger.info(message);
        }

        /**
         * Log a message with the INFO level.
         *
         * @param message Message to log.
         * @param error Associated exception.
         */
        public void info(String message, Throwable error) { 
            logger.info(message, error);
        }

        /**
         * Log a message with the WARN level.
         *
         * @param message Message to log.
         */
        public void warn(String message) { 
            logger.warn(message);
        }

        /**
         * Log a message with the WARN level.
         *
         * @param message Message to log.
         * @param error Associated exception.
         */
        public void warn(String message, Throwable error) { 
            logger.warn(message, error);
        }

        /**
         * Log a message with the ERROR level.
         *
         * @param message Message to log.
         */
        public void error(String message) { 
            logger.error(message);
        }

        /**
         * Log a message with the ERROR level.
         *
         * @param message Message to log.
         * @param error Associated exception.
         */
        public void error(String message, Throwable error) { 
            logger.error(message, error);
        }
        
    }
    
}