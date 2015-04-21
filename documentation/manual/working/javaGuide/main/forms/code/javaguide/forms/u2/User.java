/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms.u2;

import play.data.validation.Constraints.Required;

//#user
public class User {

    @Required
    protected String email;
    protected String password;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
    	this.password = password;
    }

    public String getPassword() {
        return password;
    }

}
//#user
