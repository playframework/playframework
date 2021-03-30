/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
  public Result routeParamsReqAttr(final Http.Request req, Long id, String clazz, java.util.Optional<Integer> page, List<String> items, List<String> books, Integer section) {

    final String actionMethodParams =
      "id=" + id + "[" + id.getClass().getName() + "]|" +
      "class=" + clazz + "[" + clazz.getClass().getName() + "]|" +
      "page=" + page + "[" + page.getClass().getName() + "]|" +
      "items=" + items + "[" + items.getClass().getName() + "]|" +
      "books=" + books + "[" + books.getClass().getName() + "]|" +
      "section=" + section + "[" + section.getClass().getName() + "]";

    final String javaAttrRouteParams = req.attrs().getOptional(play.routing.Router.Attrs.ROUTE_PARAMS).map(routeParamsMap ->
      routeParamsMap.iterator().map(entryTuple -> entryTuple._1 + "=" + entryTuple._2 + "[" + entryTuple._2.getClass().getName() + "]").mkString("|")
    ).get();

    final String scalaAttrRouteParams = req.asScala().attrs().get(play.api.routing.Router.Attrs$.MODULE$.RouteParams()).map(routeParamsMap ->
      routeParamsMap.iterator().map(entryTuple -> entryTuple._1 + "=" + entryTuple._2 + "[" + entryTuple._2.getClass().getName() + "]").mkString("|")
    ).get();

    return ok(
      replaceScalaCollectionWrappers(
        actionMethodParams + "\n" +
        javaAttrRouteParams + "\n" +
        scalaAttrRouteParams + "\n"
      )
    );
  }

  // The scripted tests run with Scala 2.12 and 2.13, which use different collection implementations
  private static String replaceScalaCollectionWrappers(String source) {
    return source
      .replace("scala.collection.convert.JavaCollectionWrappers$SeqWrapper", "List") // used in Scala 2.13
      .replace("scala.collection.convert.Wrappers$SeqWrapper", "List"); // used in Scala 2.12
  }
}
