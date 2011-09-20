package play.data.validation;

import javax.validation.*;
import javax.validation.metadata.*;

import play.data.validation.*;

public class Validation {
    
    private final static ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
    
    public static Validator getValidator() {
        return factory.getValidator();
    }
    
    public static <T> Error validate(T value, String name, Constraints.Validator<T> validator) {
        if(!validator.isValid(value)) {
            
        }
        return null;
    }
    
}