/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import models.UserPathBindable;
import models.UserQueryBindable;
import play.mvc.Result;

public class UserController extends ParamController<UserQueryBindable> {
    public Result path(UserPathBindable x) {
        return ok(x.toString());
    }
}
