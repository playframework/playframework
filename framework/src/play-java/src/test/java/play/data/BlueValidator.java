/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;


import play.data.validation.Constraints;
import play.libs.F;


public class BlueValidator extends Constraints.Validator<String> {

    public boolean isValid(String value) {
        return "blue".equals(value);
    }

    public F.Tuple<String, Object[]> getErrorMessageKey() {
        return F.Tuple("notblue", new Object[] {});
    }
}
