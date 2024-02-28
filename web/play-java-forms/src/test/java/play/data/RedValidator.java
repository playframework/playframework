/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import jakarta.validation.ConstraintValidator;
import play.data.validation.Constraints;
import play.libs.F;

public class RedValidator extends Constraints.Validator<Red>
    implements ConstraintValidator<ValidateRed, Red> {

  public void initialize(ValidateRed constraintAnnotation) {}

  public boolean isValid(Red value) {
    return "red".equals(value.name);
  }

  public F.Tuple<String, Object[]> getErrorMessageKey() {
    return F.Tuple("notred", new Object[] {});
  }
}
