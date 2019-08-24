/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers;

import play.mvc.*;
import java.util.List;
import java.util.stream.Collectors;
import models.UserId;

public class Application extends Controller {

  public Result index(Http.Request request) {
    return ok(request.uri());
  }
  public Result post(Http.Request r) {
    return ok(r.uri());
  }
  public Result withParam(Http.Request req, String param) {
    return ok(req.uri() + " " + param);
  }
  public Result user(UserId userId, Http.Request req) {
    return ok(req.uri() + " " + userId.id());
  }
  public Result queryUser(Http.Request req, UserId userId) {
    return ok(req.uri() + " " + userId.id());
  }
  public Result takeInt(Http.Request req, Integer i) {
    return ok(req.uri() + " " + i);
  }
  public Result takeBool(Boolean b, Http.Request req) {
    return ok(req.uri() + " " + b);
  }
  public Result takeBool2(Boolean b, Http.Request req) {
    return ok(req.uri() + " " + b);
  }
  public Result takeList(Http.Request req, List<Integer> x) {
    return ok(req.uri() + " " + x.stream().map(i -> i.toString()).collect(Collectors.joining(",")));
  }
  public Result takeJavaList(List<Integer> x, Http.Request req) {
    return ok(req.uri() + " " + x.stream().map(i -> i.toString()).collect(Collectors.joining(",")));
  }
  public Result urlcoding(String dynamic, String stat, Http.Request req, String query) {
    return ok(req.uri() + " " + "dynamic=" + dynamic + " static=" + stat + " query=" + query);
  }
  public Result route(Http.Request req, String parameter) {
    return ok(req.uri() + " " + parameter);
  }
  public Result routetest(Http.Request req, String parameter) {
    return ok(req.uri() + " " + parameter);
  }
  public Result routedefault(Http.Request req, String parameter) {
    return ok(req.uri() + " " + parameter);
  }
  public Result hello(Http.Request req) {
    return ok(req.uri() + " " + "Hello world!");
  }
  public Result interpolatorWarning(Http.Request req, String parameter) {
    return ok(req.uri() + " " + parameter);
  }
}
