/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.groups;

// #user
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;
import play.data.validation.ValidationError;
import javax.validation.groups.Default;

@Validate(groups = {SignUpCheck.class})
public class PartialUserForm implements Validatable<ValidationError> {

  @Constraints.Required(groups = {Default.class, SignUpCheck.class, LoginCheck.class})
  @Constraints.Email(groups = {Default.class, SignUpCheck.class})
  private String email;

  @Constraints.Required private String firstName;

  @Constraints.Required private String lastName;

  @Constraints.Required(groups = {SignUpCheck.class, LoginCheck.class})
  private String password;

  @Constraints.Required(groups = {SignUpCheck.class})
  private String repeatPassword;

  @Override
  public ValidationError validate() {
    if (!checkPasswords(password, repeatPassword)) {
      return new ValidationError("repeatPassword", "Passwords do not match");
    }
    return null;
  }

  // getters and setters

  // ###skip: 44
  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRepeatPassword() {
    return this.repeatPassword;
  }

  public void setRepeatPassword(String repeatPassword) {
    this.repeatPassword = repeatPassword;
  }

  private static boolean checkPasswords(final String pw1, final String pw2) {
    return false;
  }
}
// #user
