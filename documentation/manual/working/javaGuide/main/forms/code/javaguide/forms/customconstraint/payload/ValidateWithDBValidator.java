/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.payload;

//#constraint
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import play.data.validation.Constraints.PlayConstraintValidator;
import play.data.validation.Constraints.PlayConstraintValidatorWithPayload;
import play.data.validation.Constraints.ValidationPayload;

import play.db.Database;

public class ValidateWithDBValidator implements PlayConstraintValidatorWithPayload<ValidateWithDB, ValidatableWithDB<?>> {

    private final Database db;

    @Inject
    public ValidateWithDBValidator(final Database db) {
        this.db = db;
    }

    @Override
    public void initialize(final ValidateWithDB constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithDB<?> value, final ValidationPayload payload, final ConstraintValidatorContext constraintValidatorContext) {
        return reportValidationStatus(value.validate(this.db, payload), constraintValidatorContext);
    }
}
//#constraint
