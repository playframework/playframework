/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint;

//#constraint
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import play.data.validation.Constraints.PlayConstraintValidator;
//import play.data.validation.Constraints.PlayConstraintValidatorWithPayload;
//import play.data.validation.Constraints.ValidatorPayload;

import play.db.Database;

public class ValidateWithDBValidator implements PlayConstraintValidator<ValidateWithDB, ValidatableWithDB<?>> {
// ...or implement PlayConstraintValidatorWithPayload instead if you want to pass a payload

    private final Database db;

    @Inject
    public ValidateWithDBValidator(final Database db) {
        this.db = db;
    }

    @Override
    public void initialize(final ValidateWithDB constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithDB<?> value, final ConstraintValidatorContext constraintValidatorContext) {
        return reportValidationStatus(value.validate(this.db), constraintValidatorContext);
    }

    // or, if you want to pass a payload:
    //@Override
    //public boolean isValid(final ValidatableWithDB<?> value, final ValidatorPayload payload, final ConstraintValidatorContext constraintValidatorContext) {
    //    return reportValidationStatus(value.validate(this.db, payload), constraintValidatorContext);
    //}
}
//#constraint
