/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data.validation;

import javax.validation.*;
import javax.validation.metadata.*;

import play.data.validation.*;

/**
 * Validation helpers.
 */
public class Validation {
    
    /**
     * The underlying JSR-303 validator.
     */
    private final static ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
    
    /**
     * Returns a JSR-303 Validator.
     */
    public static Validator getValidator() {
        return factory.getValidator();
    }
    
    
}
