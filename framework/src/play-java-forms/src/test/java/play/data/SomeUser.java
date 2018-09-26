/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import javax.validation.groups.Default;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;

import play.data.validation.ValidationError;

@Validate
public class SomeUser implements Validatable<ValidationError> {

    @Required(groups = {Default.class, LoginCheck.class})
    @Email(groups = {LoginCheck.class})
    @MaxLength(255)
    private String email;

    @Required
    @MaxLength(255)
    private String firstName;

    @Required(groups = {Default.class})
    @MinLength(2)
    @MaxLength(255)
    private String lastName;
    
    @Required(groups = {PasswordCheck.class, LoginCheck.class})
    @MinLength(5)
    @MaxLength(255)
    private String password;
    
    @Required(groups = {PasswordCheck.class})
    @MinLength(5)
    @MaxLength(255)
    private String repeatPassword;

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

    @Override
    public ValidationError validate() {
        if (this.password != null && this.repeatPassword != null && !this.password.equals(this.repeatPassword)) {
            return new ValidationError("password", "Passwords do not match");
        }
        return null;
    }

}
