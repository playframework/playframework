/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.ValidateWith;

import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;

@Validate
public class LoginUser extends UserBase implements Validatable<String> {

    @Pattern("[0-9]")
    @ValidateWith(value = play.data.validation.Constraints.RequiredValidator.class)
    @Required
    @MinLength(255)
    @Email
    @MaxLength(255)
    private String email;


    @Required
    @MaxLength(255)
    @Email
    @play.data.format.Formats.NonEmpty // not a constraint annotation
    @MinLength(255)
    @Pattern("[0-9]")
    @ValidateWith(value = play.data.validation.Constraints.RequiredValidator.class)
    private String name;

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String validate() {
        if (this.email != null && !this.email.equals("bill.gates@microsoft.com")) {
            return "Invalid email provided!";
        }
        return ""; // for testing purposes only we return an empty string here which will also be seen as an error
    }

}