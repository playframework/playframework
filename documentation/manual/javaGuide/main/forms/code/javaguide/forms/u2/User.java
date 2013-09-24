/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms.u2;

import play.data.validation.Constraints.Required;

//#user
public class User {

    @Required
    public String email;
    public String password;
}
//#user