/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers;

import play.mvc.*;
import java.util.List;
import java.util.stream.Collectors;
import models.UserId;

public class Application extends Controller {

  public Result index() {
    return ok();
  }
  public Result post() {
    return ok();
  }
  public Result withParam(String param) {
    return ok(param);
  }
  public Result user(UserId userId) {
    return ok(userId.id());
  }
  public Result queryUser(UserId userId) {
    return ok(userId.id());
  }
  public Result takeInt(Integer i) {
    return ok("" + i);
  }
  public Result takeBool(Boolean b) {
    return ok("" + b);
  }
  public Result takeBool2(Boolean b) {
    return ok("" + b);
  }
  public Result takeList(List<Integer> x) {
    return ok(x.stream().map(i -> i.toString()).collect(Collectors.joining(",")));
  }
  public Result takeJavaList(List<Integer> x) {
    return ok(x.stream().map(i -> i.toString()).collect(Collectors.joining(",")));
  }
  public Result urlcoding(String dynamic, String stat, String query) {
    return ok("dynamic=" + dynamic + " static=" + stat + " query=" + query);
  }
  public Result route(String parameter) {
    return ok(parameter);
  }
  public Result routetest(String parameter) {
    return ok(parameter);
  }
  public Result routedefault(String parameter) {
    return ok(parameter);
  }
  public Result hello() {
    return ok("Hello world!");
  }
  public Result interpolatorWarning(String parameter) {
    return ok(parameter);
  }
}
