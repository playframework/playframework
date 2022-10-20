/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
