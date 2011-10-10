package play.data.validation;

import play.libs.F.*;
import static play.libs.F.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.validation.*;
import javax.validation.metadata.*;

import java.util.*;

public class Constraints {

    public static abstract class Validator<T> {
        public abstract boolean isValid(T object);
        public boolean isValid(T object, ConstraintValidatorContext constraintContext) {
            return isValid(object);
        }
    }

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

    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = RequiredValidator.class)
    @play.data.Form.Display(name="constraint.required")
    public static @interface Required {
        String message() default RequiredValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

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

    public static Validator<Object> required() {
        return new RequiredValidator();
    }

    // --- Min

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

    public static Validator<Number> min(long value) {
        return new MinValidator(value);
    }


}