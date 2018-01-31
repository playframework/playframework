/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static play.libs.F.Tuple;

import java.lang.annotation.*;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;

import play.api.i18n.Lang;
import play.data.validation.Constraints.Validator;
import play.i18n.MessagesApi;
import play.mvc.Http;

import org.springframework.context.i18n.LocaleContextHolder;

public class TestConstraints {

    /**
     * Defines a I18N constraint for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = I18NConstraintValidator.class)
    @Repeatable(play.data.validation.TestConstraints.I18Constraint.List.class)
    @play.data.Form.Display(name="constraint.i18nconstraint", attributes={"value"})
    public static @interface I18Constraint {
        String message() default I18NConstraintValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();

        /**
         * Defines several {@code @I18Constraint} annotations on the same element.
         */
        @Target({FIELD})
        @Retention(RUNTIME)
        public @interface List {
            I18Constraint[] value();
        }
    }

    /**
     * Validator for <code>@I18Constraint</code> fields.
     */
    public static class I18NConstraintValidator extends Validator<String> implements ConstraintValidator<I18Constraint, String> {

        String msgKey;

        final static public String message = "error.i18nconstraint";

        @Inject
        private MessagesApi messagesApi;

        public I18NConstraintValidator() {}

        @Override
        public void initialize(I18Constraint constraintAnnotation) {
            this.msgKey = constraintAnnotation.value();
        }

        @Override
        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return Pattern.compile(this.messagesApi.get(Http.Context.current.get().lang(), this.msgKey)).matcher(object).matches();
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { this.msgKey });
        }

    }

    /**
     * Defines another I18N constraint for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = AnotherI18NConstraintValidator.class)
    @Repeatable(play.data.validation.TestConstraints.AnotherI18NConstraint.List.class)
    @play.data.Form.Display(name="constraint.anotheri18nconstraint", attributes={"value"})
    public static @interface AnotherI18NConstraint {
        String message() default AnotherI18NConstraintValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();

        /**
         * Defines several {@code @AnotherI18NConstraint} annotations on the same element.
         */
        @Target({FIELD})
        @Retention(RUNTIME)
        public @interface List {
            AnotherI18NConstraint[] value();
        }
    }

    /**
     * Validator for <code>@AnotherI18NConstraint</code> fields.
     */
    public static class AnotherI18NConstraintValidator extends Validator<String> implements ConstraintValidator<AnotherI18NConstraint, String> {

        String msgKey;

        final static public String message = "error.anotheri18nconstraint";

        @Inject
        private MessagesApi messagesApi;

        public AnotherI18NConstraintValidator() {}

        @Override
        public void initialize(AnotherI18NConstraint constraintAnnotation) {
            this.msgKey = constraintAnnotation.value();
        }

        @Override
        public boolean isValid(String object) {
            if(object == null || object.length() == 0) {
                return true;
            }

            return Pattern.compile(this.messagesApi.get(new Lang(LocaleContextHolder.getLocale()), this.msgKey)).matcher(object).matches();
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[] { this.msgKey });
        }

    }
    
}