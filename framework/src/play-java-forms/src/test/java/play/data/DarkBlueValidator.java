/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;


import play.data.validation.Constraints;
import play.libs.F;


public class DarkBlueValidator extends Constraints.Validator<String> {

    public boolean isValid(String value) {
        return "darkblue".equals(value);
    }

    public F.Tuple<String, Object[]> getErrorMessageKey() {
        return F.Tuple("notdarkblue", null);
    }
}
