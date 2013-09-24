/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package validator;

import play.data.validation.*;
import static play.libs.F.*;

public class NotEmpty extends Constraints.Validator<String>{
    public boolean isValid(String s) {
        return s != null &&  s.trim().length() > 0;
    }
    public Tuple<String, Object[]> getErrorMessageKey() {
        return Tuple("error.invalid", new Object[] {});
    }
}
