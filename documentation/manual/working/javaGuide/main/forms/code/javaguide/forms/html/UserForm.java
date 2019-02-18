/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.html;

import java.util.List;

// #code
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
// #code
