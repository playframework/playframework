package play.data.validation;

public class ValidationError {
    
    private boolean isBindingFailure;
    private String[] codes;
    private Object[] arguments;
    private String defaultMessage;
    
    public ValidationError(boolean isBindingFailure, String[] codes, Object[] arguments, String defaultMessage) {
        this.isBindingFailure = isBindingFailure;
        this.codes = codes;
        this.arguments = arguments;
        this.defaultMessage = defaultMessage;
    }
    
    public String[] codes() {
        return codes;
    }
    
    public String code() {
        if(codes.length > 0) {
            return codes[0];
        }
        return null;
    }
    
    public Object[] arguments() {
        return codes;
    }
    
    public String message() {
        return defaultMessage;
    }
    
    public boolean isBindingFailure() {
        return isBindingFailure;
    }
    
    public String toString() {
        if(isBindingFailure) {
            return "invalid value";
        } else {
            return message();
        }
    }
    
}