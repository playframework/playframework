/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import com.typesafe.config.Config;

import play.i18n.Lang;
import play.i18n.Messages;
import play.data.Form.Display;

import static play.libs.F.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;

import javax.validation.*;
import javax.validation.metadata.*;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import play.libs.typedmap.TypedMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines a set of built-in validation constraints.
 */
public class Constraints {

    /**
     * Super-type for validators.
     */
    public static abstract class Validator<T> {

        /**
         * @param object    the value to test.
         * @return {@code true} if this value is valid.
         */
        public abstract boolean isValid(T object);

        /**
         * @param object            the object to check
         * @param constraintContext The JSR-303 validation context.
         * @return {@code true} if this value is valid for the given constraint.
         */
        public boolean isValid(T object, ConstraintValidatorContext constraintContext) {
            return isValid(object);
        }

        public abstract Tuple<String, Object[]> getErrorMessageKey();

    }

    /**
     * Super-type for validators with a payload.
     */
    public static abstract class ValidatorWithPayload<T> {

        /**
         * @param object    the value to test.
         * @param payload   the payload providing validation context information.
         * @return {@code true} if this value is valid.
         */
        public abstract boolean isValid(T object, ValidationPayload payload);

        /**
         * @param object            the object to check
         * @param constraintContext The JSR-303 validation context.
         * @return {@code true} if this value is valid for the given constraint.
         */
        public boolean isValid(T object, ConstraintValidatorContext constraintContext) {
            return isValid(object, constraintContext.unwrap(HibernateConstraintValidatorContext.class).getConstraintValidatorPayload(ValidationPayload.class));
        }

        public abstract Tuple<String, Object[]> getErrorMessageKey();

    }

    public static class ValidationPayload {
        private final Lang lang;
        private final Messages messages;
        private final Map<String, Object> args;
        private final TypedMap attrs;
        private final Config config;

        public ValidationPayload(final Lang lang, final Messages messages, final TypedMap attrs, final Config config) {
            this(lang, messages, Collections.emptyMap(), attrs, config);
        }

        /**
         * @deprecated Deprecated as of 2.7.0. Use {@link #ValidationPayload(Lang, Messages, TypedMap, Config)} instead.
         */
        @Deprecated
        public ValidationPayload(final Lang lang, final Messages messages, final Map<String, Object> args, final TypedMap attrs, final Config config) {
            this.lang = lang;
            this.messages = messages;
            this.args = args;
            this.attrs = attrs;
            this.config = config;
        }

        /**
         * @return if validation happens during a Http Request the lang of that request, otherwise null
         */
        public Lang getLang() {
            return this.lang;
        }

        /**
         * @return if validation happens during a Http Request the messages for the lang of that request, otherwise null
         */
        public Messages getMessages() {
            return this.messages;
        }

        /**
         * @return if validation happens during a Http Request the args map of that request, otherwise null
         *
         * @deprecated Use {@link #getAttrs()} instead. Since 2.7.0.
         */
        @Deprecated
        public Map<String, Object> getArgs() {
            return this.args;
        }

        /**
         * @return if validation happens during a Http Request the request attributes of that request, otherwise null
         */
        public TypedMap getAttrs() {
            return this.attrs;
        }

        /**
         * @return the current application configuration, will always be set, even when accessed outside a Http Request
         */
        public Config getConfig() {
            return this.config;
        }
    }

    /**
     * Converts a set of constraints to human-readable values.
     * Does not guarantee the order of the returned constraints.
     *
     * This method calls {@code displayableConstraint} under the hood.
     *
     * @param constraints    the set of constraint descriptors.
     * @return a list of pairs of tuples assembled from displayableConstraint.
     */
    public static List<Tuple<String,List<Object>>> displayableConstraint(Set<ConstraintDescriptor<?>> constraints) {
        return constraints.parallelStream().filter(c -> c.getAnnotation().annotationType().isAnnotationPresent(Display.class)).map(c -> displayableConstraint(c)).collect(Collectors.toList());
    }
    
    /**
     * Converts a set of constraints to human-readable values in guaranteed order.
     * Only constraints that have an annotation that intersect with the {@code orderedAnnotations} parameter will be considered.
     * The order of the returned constraints corresponds to the order of the {@code orderedAnnotations parameter}.
     * @param constraints           the set of constraint descriptors.
     * @param orderedAnnotations    the array of annotations
     * @return a list of tuples showing readable constraints.
     */
    public static List<Tuple<String,List<Object>>> displayableConstraint(Set<ConstraintDescriptor<?>> constraints, Annotation[] orderedAnnotations) {
        final List<Annotation> constraintAnnot = constraints.stream().
            map(c -> c.getAnnotation()).
            collect(Collectors.<Annotation>toList());

        return Stream
                .of(orderedAnnotations)
                .filter(constraintAnnot::contains) // only use annotations for which we actually have a constraint
                .filter(a -> a.annotationType().isAnnotationPresent(Display.class))
                .map(a -> displayableConstraint(
                        constraints.parallelStream()
                                .filter(c -> c.getAnnotation().equals(a))
                                .findFirst()
                                .get()
                        )
                ).collect(Collectors.toList());
    }
    
    /**
     * Converts a constraint to a human-readable value.
     *
     * @param constraint    the constraint descriptor.
     * @return A tuple containing the constraint's display name and the constraint attributes.
     */
    public static Tuple<String,List<Object>> displayableConstraint(ConstraintDescriptor<?> constraint) {
        final Display displayAnnotation = constraint.getAnnotation().annotationType().getAnnotation(Display.class);
        return Tuple(displayAnnotation.name(), Collections.unmodifiableList(Stream.of(displayAnnotation.attributes()).map(attr -> constraint.getAttributes().get(attr)).collect(Collectors.toList())));
    }

    // --- Required

    /**
     * Defines a field as required.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = RequiredValidator.class)
    @Repeatable(play.data.validation.Constraints.Required.List.class)
    @Display(name="constraint.required")
    public @interface Required {
        String message() default RequiredValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        /**
         * Defines several {@code @Required} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Required[] value();
        }
    }

    /**
     * Validator for {@code @Required} fields.
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
     * @return the RequiredValidator
     */
    public static Validator<Object> required() {
        return new RequiredValidator();
    }

    // --- Min

    /**
     * Defines a minimum value for a numeric field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MinValidator.class)
    @Repeatable(play.data.validation.Constraints.Min.List.class)
    @Display(name="constraint.min", attributes={"value"})
    public @interface Min {
        String message() default MinValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();

        /**
         * Defines several {@code @Min} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Min[] value();
        }
    }

    /**
     * Validator for {@code @Min} fields.
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
     *
     * @param value the minimum value
     * @return a validator for number.
     */
    public static Validator<Number> min(long value) {
        return new MinValidator(value);
    }

    // --- Max

    /**
     * Defines a maximum value for a numeric field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MaxValidator.class)
    @Repeatable(play.data.validation.Constraints.Max.List.class)
    @Display(name="constraint.max", attributes={"value"})
    public @interface Max {
        String message() default MaxValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();

        /**
         * Defines several {@code @Max} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Max[] value();
        }
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
     *
     * @param value maximum value
     * @return a validator using MaxValidator.
     */
    public static Validator<Number> max(long value) {
        return new MaxValidator(value);
    }

    // --- MinLength

    /**
     * Defines a minimum length for a string field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MinLengthValidator.class)
    @Repeatable(play.data.validation.Constraints.MinLength.List.class)
    @Display(name="constraint.minLength", attributes={"value"})
    public @interface MinLength {
        String message() default MinLengthValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();

        /**
         * Defines several {@code @MinLength} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            MinLength[] value();
        }
    }

    /**
     * Validator for {@code @MinLength} fields.
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
            if(object == null || object.isEmpty()) {
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
     * @param value    the minimum length value.
     * @return the MinLengthValidator
     */
    public static Validator<String> minLength(long value) {
        return new MinLengthValidator(value);
    }

    // --- MaxLength

    /**
     * Defines a maximum length for a string field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = MaxLengthValidator.class)
    @Repeatable(play.data.validation.Constraints.MaxLength.List.class)
    @Display(name="constraint.maxLength", attributes={"value"})
    public @interface MaxLength {
        String message() default MaxLengthValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        long value();

        /**
         * Defines several {@code @MaxLength} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            MaxLength[] value();
        }
    }

    /**
     * Validator for {@code @MaxLength} fields.
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
            if(object == null || object.isEmpty()) {
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
     * @param value    the max length
     * @return the MaxLengthValidator
     */
    public static Validator<String> maxLength(long value) {
        return new MaxLengthValidator(value);
    }

    // --- Email

    /**
     * Defines a email constraint for a string field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = EmailValidator.class)
    @Repeatable(play.data.validation.Constraints.Email.List.class)
    @Display(name="constraint.email", attributes={})
    public @interface Email {
        String message() default EmailValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        /**
         * Defines several {@code @Email} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Email[] value();
        }
    }

    /**
     * Validator for {@code @Email} fields.
     */
    public static class EmailValidator extends Validator<String> implements ConstraintValidator<Email, String> {

        final static public String message = "error.email";
        final static java.util.regex.Pattern regex = java.util.regex.Pattern.compile(
            "\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\b");

        public EmailValidator() {}

        @Override
        public void initialize(Email constraintAnnotation) {
        }

        @Override
        public boolean isValid(String object) {
            if (object == null || object.isEmpty()) {
                return true;
            }

            return regex.matcher(object).matches();
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] {});
        }

    }

    /**
     * Constructs a 'email' validator.
     * @return the EmailValidator
     */
    public static Validator<String> email() {
        return new EmailValidator();
    }

    // --- Pattern

    /**
     * Defines a pattern constraint for a string field.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = PatternValidator.class)
    @Repeatable(play.data.validation.Constraints.Pattern.List.class)
    @Display(name="constraint.pattern", attributes={"value"})
    public @interface Pattern {
        String message() default PatternValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();

        /**
         * Defines several {@code @Pattern} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Pattern[] value();
        }
    }

    /**
     * Validator for {@code @Pattern} fields.
     */
    public static class PatternValidator extends Validator<String> implements ConstraintValidator<Pattern, String> {

        final static public String message = "error.pattern";
        java.util.regex.Pattern regex = null;

        public PatternValidator() {}

        public PatternValidator(String regex) {
            this.regex = java.util.regex.Pattern.compile(regex);
        }

        @Override
        public void initialize(Pattern constraintAnnotation) {
            regex = java.util.regex.Pattern.compile(constraintAnnotation.value());
        }

        @Override
        public boolean isValid(String object) {
            if (object == null || object.isEmpty()) {
                return true;
            }

            return regex.matcher(object).matches();
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { regex });
        }

    }

    /**
     * Constructs a 'pattern' validator.
     * @param regex    the regular expression to match.
     * @return the PatternValidator.
     */
    public static Validator<String> pattern(String regex) {
        return new PatternValidator(regex);
    }

    // --- validate fields with custom validator
    
    /**
     * Defines a custom validator.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidateWithValidator.class)
    @Repeatable(play.data.validation.Constraints.ValidateWith.List.class)
    @Display(name="constraint.validatewith", attributes={})
    public @interface ValidateWith {
        String message() default ValidateWithValidator.defaultMessage;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        Class<? extends Validator> value();

        /**
         * Defines several {@code @ValidateWith} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            ValidateWith[] value();
        }
    }

    /**
     * Validator for {@code @ValidateWith} fields.
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

    // --- validate fields with custom validator that gets payload
    
    /**
     * Defines a custom validator.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidatePayloadWithValidator.class)
    @Repeatable(play.data.validation.Constraints.ValidatePayloadWith.List.class)
    @Display(name="constraint.validatewith", attributes={})
    public @interface ValidatePayloadWith {
        String message() default ValidatePayloadWithValidator.defaultMessage;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        Class<? extends ValidatorWithPayload> value();

        /**
         * Defines several {@code @ValidatePayloadWith} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            ValidatePayloadWith[] value();
        }
    }

    /**
     * Validator for {@code @ValidatePayloadWith} fields.
     */
    public static class ValidatePayloadWithValidator extends ValidatorWithPayload<Object> implements ConstraintValidator<ValidatePayloadWith, Object> {

        final static public String defaultMessage = "error.invalid";
        Class<?> clazz = null;
        ValidatorWithPayload validator = null;

        public ValidatePayloadWithValidator() {}

        public ValidatePayloadWithValidator(Class clazz) {
            this.clazz = clazz;
        }

        public void initialize(ValidatePayloadWith constraintAnnotation) {
            this.clazz = constraintAnnotation.value();
             try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                validator = (ValidatorWithPayload)constructor.newInstance();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean isValid(Object object, ValidationPayload payload) {
            try {
                return validator.isValid(object, payload);
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

    // --- class level helpers

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidateValidator.class)
    @Repeatable(play.data.validation.Constraints.Validate.List.class)
    public @interface Validate {
        String message() default "error.invalid";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        /**
         * Defines several {@code @Validate} annotations on the same element.
         */
        @Target({TYPE, ANNOTATION_TYPE})
        @Retention(RUNTIME)
        public @interface List {
            Validate[] value();
        }
    }

    public interface Validatable<T> {
        T validate();
    }

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidateValidatorWithPayload.class)
    @Repeatable(play.data.validation.Constraints.ValidateWithPayload.List.class)
    public @interface ValidateWithPayload {
        String message() default "error.invalid";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        /**
         * Defines several {@code @ValidateWithPayload} annotations on the same element.
         */
        @Target({TYPE, ANNOTATION_TYPE})
        @Retention(RUNTIME)
        public @interface List {
            ValidateWithPayload[] value();
        }
    }

    public interface ValidatableWithPayload<T> {
        T validate(ValidationPayload payload);
    }

    public static class ValidateValidator implements PlayConstraintValidator<Validate, Validatable<?>> {

        @Override
        public void initialize(final Validate constraintAnnotation) {
        }

        @Override
        public boolean isValid(final Validatable<?> value, final ConstraintValidatorContext constraintValidatorContext) {
            return reportValidationStatus(value.validate(), constraintValidatorContext);
        }
    }

    public static class ValidateValidatorWithPayload implements PlayConstraintValidatorWithPayload<ValidateWithPayload, ValidatableWithPayload<?>> {

        @Override
        public void initialize(final ValidateWithPayload constraintAnnotation) {
        }

        @Override
        public boolean isValid(final ValidatableWithPayload<?> value, final ValidationPayload payload, final ConstraintValidatorContext constraintValidatorContext) {
            return reportValidationStatus(value.validate(payload), constraintValidatorContext);
        }
    }

    public interface PlayConstraintValidator<A extends Annotation, T> extends ConstraintValidator<A, T> {

        default boolean validationSuccessful(final Object validationResult) {
            return validationResult == null || (validationResult instanceof List && ((List<?>)validationResult).isEmpty());
        }

        default boolean reportValidationStatus(final Object validationResult, final ConstraintValidatorContext constraintValidatorContext) {
            if(validationSuccessful(validationResult)) {
                return true;
            }
            constraintValidatorContext
                .unwrap(HibernateConstraintValidatorContext.class)
                .withDynamicPayload(validationResult);
            return false;
        }
    }

    public interface PlayConstraintValidatorWithPayload<A extends Annotation, T> extends PlayConstraintValidator<A, T> {

        @Override
        default boolean isValid(final T value, final ConstraintValidatorContext constraintValidatorContext) {
            return isValid(value, constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class).getConstraintValidatorPayload(ValidationPayload.class), constraintValidatorContext);
        }

        boolean isValid(final T value, final ValidationPayload payload, final ConstraintValidatorContext constraintValidatorContext);
    }
}
