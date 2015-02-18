/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data.validation;

import play.libs.F.*;
import static play.libs.F.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

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

        public abstract Tuple<String, Object[]> getErrorMessageKey();

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

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] {});
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

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { min });
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

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { max });
        }

    }

    /**
     * Constructs a 'max' validator.
     */
    public static Validator<Number> max(long value) {
        return new MaxValidator(value);
    }

    // --- MinLength

    /**
     * Defines a minumum length for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MinLengthValidator.class)
    @play.data.Form.Display(name="constraint.minLength", attributes={"value"})
    public static @interface MinLength {
        String message() default MinLengthValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();
    }

    /**
     * Validator for <code>@MinLength</code> fields.
     */
    public static class MinLengthValidator extends Validator<String> implements ConstraintValidator<MinLength, String> {

        final static public String message = "error.minLength";
        private long min;

        public MinLengthValidator() {}

        public MinLengthValidator(long value) {
            this.min = value;
        }

        public void initialize(MinLength constraintAnnotation) {
            this.min = constraintAnnotation.value();
        }

        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return object.length() >= min;
        }

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { min });
        }

    }

    /**
     * Constructs a 'minLength' validator.
     */
    public static Validator<String> minLength(long value) {
        return new MinLengthValidator(value);
    }

    // --- MaxLength

    /**
     * Defines a maxmimum length for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MaxLengthValidator.class)
    @play.data.Form.Display(name="constraint.maxLength", attributes={"value"})
    public static @interface MaxLength {
        String message() default MaxLengthValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();
    }

    /**
     * Validator for <code>@MaxLength</code> fields.
     */
    public static class MaxLengthValidator extends Validator<String> implements ConstraintValidator<MaxLength, String> {

        final static public String message = "error.maxLength";
        private long max;

        public MaxLengthValidator() {}

        public MaxLengthValidator(long value) {
            this.max = value;
        }

        public void initialize(MaxLength constraintAnnotation) {
            this.max = constraintAnnotation.value();
        }

        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return object.length() <= max;
        }

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { max });
        }

    }

    /**
     * Constructs a 'maxLength' validator.
     */
    public static Validator<String> maxLength(long value) {
        return new MaxLengthValidator(value);
    }

    // --- Email

    /**
     * Defines a email constraint for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = EmailValidator.class)
    @play.data.Form.Display(name="constraint.email", attributes={})
    public static @interface Email {
        String message() default EmailValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Validator for <code>@Email</code> fields.
     */
    public static class EmailValidator extends Validator<String> implements ConstraintValidator<Email, String> {

        final static public String message = "error.email";
        final static java.util.regex.Pattern regex = java.util.regex.Pattern.compile(
            "\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\b");

        public EmailValidator() {}

        public void initialize(Email constraintAnnotation) {
        }

        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return regex.matcher(object).matches();
        }

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] {});
        }

    }

    /**
     * Constructs a 'email' validator.
     */
    public static Validator<String> email() {
        return new EmailValidator();
    }

    // --- Pattern

    /**
     * Defines a pattern constraint for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = PatternValidator.class)
    @play.data.Form.Display(name="constraint.pattern", attributes={"value"})
    public static @interface Pattern {
        String message() default PatternValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();
    }

    /**
     * Validator for <code>@Pattern</code> fields.
     */
    public static class PatternValidator extends Validator<String> implements ConstraintValidator<Pattern, String> {

        final static public String message = "error.pattern";
        java.util.regex.Pattern regex = null;

        public PatternValidator() {}

        public PatternValidator(String regex) {
            this.regex = java.util.regex.Pattern.compile(regex);
        }

        public void initialize(Pattern constraintAnnotation) {
            regex = java.util.regex.Pattern.compile(constraintAnnotation.value());
        }

        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return regex.matcher(object).matches();
        }

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { regex });
        }

    }

    /**
     * Constructs a 'pattern' validator.
     */
    public static Validator<String> pattern(String regex) {
        return new PatternValidator(regex);
    }

     /**
     * Defines a custom validator.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidateWithValidator.class)
    @play.data.Form.Display(name="constraint.validatewith", attributes={})
    public static @interface ValidateWith {
        String message() default ValidateWithValidator.defaultMessage;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        Class<? extends Validator> value();
    }

    /**
     * Validator for <code>@ValidateWith</code> fields.
     */
    public static class ValidateWithValidator extends Validator<Object> implements ConstraintValidator<ValidateWith, Object> {

        final static public String defaultMessage = "error.invalid";
        Class<?> clazz = null;
        Validator validator = null;

        public ValidateWithValidator() {}

        public ValidateWithValidator(Class clazz) {
            this.clazz = clazz;
        }

        public void initialize(ValidateWith constraintAnnotation) {
            this.clazz = constraintAnnotation.value();
             try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                validator = (Validator)constructor.newInstance();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean isValid(Object object) {
            try {
                return validator.isValid(object);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public Tuple<String, Object[]> getErrorMessageKey() {
            Tuple<String, Object[]> errorMessageKey = null;
            try {
                errorMessageKey = validator.getErrorMessageKey();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }

            return (errorMessageKey != null) ? errorMessageKey : Tuple(defaultMessage, new Object[] {});
        }

    }

}
