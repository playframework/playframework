/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.controllers;

import javaguide.binder.models.User;
import javaguide.binder.models.AgeRange;
import play.mvc.Controller;
import play.mvc.Result;

public class BinderApplication extends Controller {

  // #path
  public Result user(User user) {
    return ok(user.name);
  }
  // #path

  // #query
  public Result age(AgeRange ageRange) {
    return ok(String.valueOf(ageRange.from));
  }
  // #query
}
