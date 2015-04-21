/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms.html;

import java.util.List;

//#code
public class UserForm {

    private String name;
    private List<String> emails;

    public void setName(String name) {
    	this.name = name;
    }

    public String getName() {
    	return name;
    }

    public void setEmails(List<String> emails) {
    	this.emails = emails;
    }

    public List<String> getEmails() {
    	return emails;
    }

}
//#code

