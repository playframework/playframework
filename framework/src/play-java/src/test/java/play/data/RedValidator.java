/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;


import play.data.validation.Constraints;
import play.libs.F;

import javax.validation.ConstraintValidator;

public class RedValidator extends Constraints.Validator<Red> implements ConstraintValidator<ValidateRed, Red> {

    public void initialize(ValidateRed constraintAnnotation) {
    }

    public boolean isValid(Red value) {
        return "red".equals(value.name);
    }

    public F.Tuple<String, Object[]> getErrorMessageKey() {
        return F.Tuple("notred", new Object[] {});
    }
}
