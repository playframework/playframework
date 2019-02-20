/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

// #constraint
import java.util.List;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import play.data.validation.Constraints.PlayConstraintValidator;

import play.db.Database;

public class ValidateWithDBValidator
    implements PlayConstraintValidator<ValidateWithDB, ValidatableWithDB<?>> {

  private final Database db;

  @Inject
  public ValidateWithDBValidator(final Database db) {
    this.db = db;
  }

  @Override
  public void initialize(final ValidateWithDB constraintAnnotation) {}

  @Override
  public boolean isValid(
      final ValidatableWithDB<?> value,
      final ConstraintValidatorContext constraintValidatorContext) {
    return reportValidationStatus(value.validate(this.db), constraintValidatorContext);
  }
}
// #constraint
