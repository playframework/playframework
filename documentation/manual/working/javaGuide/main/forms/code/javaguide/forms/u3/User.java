/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms.u3;

import play.data.validation.Constraints;
import static javaguide.forms.JavaForms.authenticate;

//#user
public class User {

    @Constraints.Required
    public String email;
    public String password;

    public String validate() {
        if (authenticate(email, password) == null) {
            return "Invalid email or password";
        }
        return null;
    }
}
//#user
