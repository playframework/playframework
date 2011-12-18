package play.data.validation;

import play.libs.F.*;
import static play.libs.F.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.validation.*;
import javax.validation.metadata.*;

import java.util.*;

/**
 * Defines a set of built-in validation constraints.
 */
public class Constraints {
    
    /**
     * Super-type for validators.
     */
    public static abstract class Validator<T> {
        
        /**
         * Returns <code>true</code> if this value is valid.
         */
        public abstract boolean isValid(T object);
        
        /**
         * Returns <code>true</code> if this value is valid for the given constraint.
         *
         * @param constraintContext The JSR-303 validation context.
         */
        public boolean isValid(T object, ConstraintValidatorContext constraintContext) {
            return isValid(object);
        }
        
    }
    
    /**
     * Converts a set of constraints to human-readable values.
     */
    public static List<Tuple<String,List<Object>>> displayableConstraint(Set<ConstraintDescriptor<?>> constraints) {
        List<Tuple<String,List<Object>>> displayable = new ArrayList<Tuple<String,List<Object>>>();
        for(ConstraintDescriptor<?> c: constraints) {
            Class<?> annotationType = c.getAnnotation().annotationType();
            if(annotationType.isAnnotationPresent(play.data.Form.Display.class)) {
                play.data.Form.Display d = annotationType.getAnnotation(play.data.Form.Display.class);
                String name = d.name();
                List<Object> attributes = new ArrayList<Object>();
                Map<String,Object> annotationAttributes = c.getAttributes();
                for(String attr: d.attributes()) {
                    attributes.add(annotationAttributes.get(attr));
                }
                displayable.add(Tuple(name, attributes)); 
            }
        }        
        return displayable;
    }
    
    // --- Required
    
    /**
     * Defines a field as required.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = RequiredValidator.class)
    @play.data.Form.Display(name="constraint.required")
    public static @interface Required {
        String message() default RequiredValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validator for <code>@Required</code> fields.
     */
    public static class RequiredValidator extends Validator<Object> implements ConstraintValidator<Required, Object> {
        
        final static public String message = "error.required";
        
        public void initialize(Required constraintAnnotation) {}
        
        public boolean isValid(Object object) {
            if(object == null) {
                return false;
            }

            if(object instanceof String) {
                return !((String)object).isEmpty();
            }

            if(object instanceof Collection) {
                return !((Collection)object).isEmpty();
            }

            return true;
        }
        
    }
    
    /**
     * Constructs a 'required' validator.
     */
    public static Validator<Object> required() {
        return new RequiredValidator();
    }
    
    // --- Min
    
    /**
     * Defines a minumum value for a numeric field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MinValidator.class)
    @play.data.Form.Display(name="constraint.min", attributes={"value"})
    public static @interface Min {
        String message() default MinValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();
    }
    
    /**
     * Validator for <code>@Min</code> fields.
     */
    public static class MinValidator extends Validator<Number> implements ConstraintValidator<Min, Number> {
        
        final static public String message = "error.min";
        private long min;
        
        public MinValidator() {}
        
        public MinValidator(long value) {
            this.min = value;
        }
        
        public void initialize(Min constraintAnnotation) {
            this.min = constraintAnnotation.value();
        }
        
        public boolean isValid(Number object) {
            if(object == null) {
                return true;
            }
            
            return object.longValue() >= min;
        }
        
    }
    
    /**
     * Constructs a 'min' validator.
     */
    public static Validator<Number> min(long value) {
        return new MinValidator(value);
    }
    
    // --- Max
    
    /**
     * Defines a maximum value for a numeric field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MaxValidator.class)
    @play.data.Form.Display(name="constraint.max", attributes={"value"})
    public static @interface Max {
        String message() default MaxValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();
    }
    
    /**
     * Validator for <code>@Max</code> fields.
     */
    public static class MaxValidator extends Validator<Number> implements ConstraintValidator<Max, Number> {
        
        final static public String message = "error.max";
        private long max;
        
        public MaxValidator() {}
        
        public MaxValidator(long value) {
            this.max = value;
        }
        
        public void initialize(Max constraintAnnotation) {
            this.max = constraintAnnotation.value();
        }
        
        public boolean isValid(Number object) {
            if(object == null) {
                return true;
            }
            
            return object.longValue() <= max;
        }
        
    }
    
    /**
     * Constructs a 'max' validator.
     */
    public static Validator<Number> max(long value) {
        return new MaxValidator(value);
    }
    
}