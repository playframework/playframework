/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.nopayload;

// #constraint
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidatorContext;
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
