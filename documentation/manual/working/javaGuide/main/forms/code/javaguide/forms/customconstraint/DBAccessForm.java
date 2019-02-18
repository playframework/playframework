/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

// #user
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.db.Database;

@ValidateWithDB
public class DBAccessForm implements ValidatableWithDB<ValidationError> {

  @Constraints.Required @Constraints.Email private String email;

  @Constraints.Required private String firstName;

  @Constraints.Required private String lastName;

  @Constraints.Required private String password;

  @Constraints.Required private String repeatPassword;

  @Override
  public ValidationError validate(final Database db) {
    // Access the database to check if the email already exists
    if (User.byEmail(email, db) != null) {
      return new ValidationError("email", "This e-mail is already registered.");
    }
    return null;
  }

  // getters and setters

  // ###skip: 46
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

  public static class User {
    public static String byEmail(String email, Database db) {
      return email;
    }
  }
}
// #user
