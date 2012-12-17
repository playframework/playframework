package play.data.validation;

import java.util.*;

import com.google.common.collect.ImmutableList;

/**
 * A form validation error.
 */
public class ValidationError {
    
    private String key;
    private String message;
    private List<Object> arguments;

    /**
     * Constructs a new <code>ValidationError</code>.
     *
     * @param key the error key
     * @param message the error message
     */
    public ValidationError(String key, String message) {
        this(key, message, ImmutableList.of());
    }
    
    /**
     * Constructs a new <code>ValidationError</code>.
     *
     * @param key the error key
     * @param message the error message
     * @param arguments the error message arguments
     */
    public ValidationError(String key, String message, List<Object> arguments) {
        this.key = key;
        this.message = message;
        this.arguments = arguments;
    }
    
    /**
     * Returns the error key.
     */
    public String key() {
        return key;
    }
    
    /**
     * Returns the error message.
     */
    public String message() {
        return message;
    }

    /**
     * Returns the error arguments.
     */
    public List<Object> arguments() {
        return arguments;
    }
    
    public String toString() {
        return "ValidationError(" + key + "," + message + "," + arguments + ")";
    }
    
}