/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

//#constraint
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import play.data.validation.ValidationError;
import play.db.Database;

public class SelfValidatingBasicWithDBValidator implements ConstraintValidator<SelfValidatingBasicWithDB, ValidatableBasicWithDB> {

    private Database db;

    @Inject
    public SelfValidatingBasicWithDBValidator(final Database db) {
        this.db = db;
    }

    @Override
    public void initialize(final SelfValidatingBasicWithDB constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableBasicWithDB value, final ConstraintValidatorContext constraintValidatorContext) {
        final ValidationError result = value.validateInstance(this.db);
        if(result == null) {
            return true;
        }
        constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class).withDynamicPayload(result);
        return false;
    }
}
//#constraint
