package play.data.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static play.libs.F.Tuple;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;

import play.data.validation.Constraints.Validator;
import play.i18n.MessagesApi;
import play.mvc.Http;

public class TestConstraints {

    /**
     * Defines a I18N constraint for a string field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = I18NConstraintValidator.class)
    @play.data.Form.Display(name="constraint.i18nconstraint", attributes={"value"})
    public static @interface I18Constraint {
        String message() default I18NConstraintValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();
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
    
}