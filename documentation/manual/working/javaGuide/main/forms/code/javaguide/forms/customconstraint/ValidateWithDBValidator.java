/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

//#constraint
import java.util.List;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import play.db.Database;

public class ValidateWithDBValidator implements ConstraintValidator<ValidateWithDB, ValidatableWithDB<?>> {

    private Database db;

    @Inject
    public ValidateWithDBValidator(final Database db) {
        this.db = db;
    }

    @Override
    public void initialize(final ValidateWithDB constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithDB<?> value, final ConstraintValidatorContext constraintValidatorContext) {
        final Object result = value.validateInstance(this.db);
        if(result == null ||
                (result instanceof List && ((List<?>)result).isEmpty()) ||
                (result instanceof String && ((String)result).trim().isEmpty())) {
            return true;
        }
        constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class).withDynamicPayload(result);
        return false;
    }
}
//#constraint
