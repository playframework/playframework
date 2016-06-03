/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.ValidateWith;

public class LoginUser extends UserBase {

    @Min(50)
    @Max(95)
    @Pattern("*")
    @ValidateWith(value = play.data.validation.Constraints.RequiredValidator.class)
    @Required
    @MinLength(255)
    @Email
    @MaxLength(255)
    private String email;


    @Required
    @MaxLength(255)
    @Email
    @Max(100)
    @play.data.format.Formats.NonEmpty // not a constraint annotation
    @MinLength(255)
    @Pattern("*")
    @ValidateWith(value = play.data.validation.Constraints.RequiredValidator.class)
    @Min(2)
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

}