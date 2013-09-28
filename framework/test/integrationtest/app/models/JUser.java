/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package models;

import play.data.*;
import play.data.validation.Constraints.*;
import validator.NotEmpty;

public class JUser {
      @ValidateWith(NotEmpty.class)
      public String email;

      //this is generated in java projects
      public String getEmail(){return email;}
      public void setEmail(String e){email = e;}
}
