package play.data.validation;

import java.util.*;

public class ValidationError {
    
    private String key;
    private String message;
    private List<Object> arguments;
    
    public ValidationError(String key, String message, List<Object> arguments) {
        this.key = key;
        this.message = message;
        this.arguments = arguments;
    }
    
    public String key() {
        return key;
    }
    
    public String message() {
        return message;
    }
    
    public List<Object> arguments() {
        return arguments;
    }
    
    public String toString() {
        return "ValidationError(" + key + "," + message + "," + arguments + ")";
    }
    
}