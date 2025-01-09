/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OptionalController extends Controller {

  public Result queryInt(OptionalInt x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsInt()) : "emptyOptionalInt");
  }

  public Result queryLong(OptionalLong x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsLong()) : "emptyOptionalLong");
  }

  public Result queryDouble(OptionalDouble x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsDouble()) : "emptyOptionalDouble");
  }

  public Result queryIntDefault(OptionalInt x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsInt()) : "emptyOptionalInt");
  }

  public Result queryLongDefault(OptionalLong x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsLong()) : "emptyOptionalLong");
  }

  public Result queryDoubleDefault(OptionalDouble x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsDouble()) : "emptyOptionalDouble");
  }
}
