/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

//#constraint
import java.util.List;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import play.data.validation.PlayConstraintValidator;

import play.db.Database;

public class ValidateWithDBValidator implements PlayConstraintValidator<ValidateWithDB, ValidatableWithDB<?>> {

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
        final Object result = value.validate(this.db);
        if(validationSuccessful(result)) {
            return true;
        }
        return reportValidationFailure(result, constraintValidatorContext);
    }
}
//#constraint
