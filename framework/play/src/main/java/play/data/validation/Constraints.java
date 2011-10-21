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
 * Defines a set of built-in constraints.
 */
public class Constraints {
    
    /**
     * Super type for Play validators.
     */
    public static abstract class Validator<T> {
        
        /**
         * Is this value valid?
         */
        public abstract boolean isValid(T object);
        
        /**
         * Is this value valid?
         *
         * @param constraintContext The JSR-303 validation context.
         */
        public boolean isValid(T object, ConstraintValidatorContext constraintContext) {
            return isValid(object);
        }
        
    }
    
    /**
     * Transform a set of constraints to something displayable in the user interface.
     */
    public static List<T2<String,List<Object>>> displayableConstraint(Set<ConstraintDescriptor<?>> constraints) {
        List<T2<String,List<Object>>> displayable = new ArrayList<T2<String,List<Object>>>();
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
                displayable.add(T2(name, attributes)); 
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
     * Validator for @required fields.
     */
    public static class RequiredValidator extends Validator<Object> implements ConstraintValidator<Required, Object> {
        
        final static public String message = "validation.required";
        
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
     * Construct a 'required' validator.
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
    @play.data.Form.Display(name="constraint.required", attributes={"value"})
    public static @interface Min {
        String message() default MinValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();
    }
    
    /**
     * Validator for @min fields.
     */
    public static class MinValidator extends Validator<Number> implements ConstraintValidator<Min, Number> {
        
        final static public String message = "validation.min";
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
     * Construct a 'min' validator.
     */
    public static Validator<Number> min(long value) {
        return new MinValidator(value);
    }
    
    
}