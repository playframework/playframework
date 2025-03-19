/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import static play.mvc.Http.HttpVerbs.DELETE;
import static play.mvc.Http.HttpVerbs.GET;
import static play.mvc.Http.HttpVerbs.HEAD;
import static play.mvc.Http.HttpVerbs.OPTIONS;
import static play.mvc.Http.HttpVerbs.PATCH;
import static play.mvc.Http.HttpVerbs.POST;
import static play.mvc.Http.HttpVerbs.PUT;

import play.mvc.Controller;
import play.mvc.Result;

public class MethodController extends Controller {

  public Result get() {
    return ok(GET);
  }

  public Result post() {
    return ok(POST);
  }

  public Result put() {
    return ok(PUT);
  }

  public Result head() {
    return ok(HEAD);
  }

  public Result delete() {
    return ok(DELETE);
  }

  public Result patch() {
    return ok(PATCH);
  }

  public Result options() {
    return ok(OPTIONS);
  }
}
