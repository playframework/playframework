package play.data.validation;

import java.util.*;

/**
 * A validation error.
 */
public class ValidationError {
    
    private String key;
    private String message;
    private List<Object> arguments;
    
    /**
     * Construct a new ValidationError.
     *
     * @param key The error key.
     * @param message The error message.
     * @param arguments The error message arguments.
     */
    public ValidationError(String key, String message, List<Object> arguments) {
        this.key = key;
        this.message = message;
        this.arguments = arguments;
    }
    
    /**
     * Retrieve the error key.
     */
    public String key() {
        return key;
    }
    
    /**
     * Retrieve the error message.
     */
    public String message() {
        return message;
    }
    
    /**
     * Retrieve the error arguments.
     */
    public List<Object> arguments() {
        return arguments;
    }
    
    public String toString() {
        return "ValidationError(" + key + "," + message + "," + arguments + ")";
    }
    
}