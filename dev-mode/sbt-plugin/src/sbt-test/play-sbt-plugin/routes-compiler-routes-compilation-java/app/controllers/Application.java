/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.WebSocket;

public class Application extends Controller {

  private final ClassLoaderExecutionContext clExecutionContext;

  @Inject
  public Application(ClassLoaderExecutionContext ec) {
    this.clExecutionContext = ec;
  }

  public CompletionStage<Result> async(Request request, Integer x) {
    return CompletableFuture.supplyAsync(() -> 2 * x, clExecutionContext.current())
        .thenApply(answer -> ok(String.format("Answer: " + answer)));
  }

  public WebSocket webSocket(String x) {
    return WebSocket.Text.accept(
        request -> Flow.fromSinkAndSource(Sink.ignore(), Source.single("Hello, " + x)));
  }

  public Result uriPattern(String x) {
    return ok(x);
  }

  public Result onlyRequestParam(Request request) {
    return ok();
  }

  public Result multiParams(
      Boolean a,
      Character b,
      String c,
      Short d,
      Integer e,
      Long f,
      Float g,
      Double h,
      UUID i,
      OptionalInt j,
      OptionalLong k,
      OptionalDouble l,
      String m,
      String n,
      String o,
      String p,
      String q,
      String r,
      String s,
      String t,
      String u,
      String v,
      String w,
      String x,
      String y,
      String z) {
    return ok(
        Stream.of(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z)
            .map(Object::toString)
            .collect(Collectors.joining(",")));
  }

  public Result urlcoding(String dynamic, String _static, String query) {
    return ok(String.format("dynamic=%s static=%s query=%s", dynamic, _static, query));
  }

  public Result keyword(String keyword) {
    return ok(keyword);
  }

  public Result keywordWithRequest(Request request, String keyword) {
    return ok(keyword);
  }

  public Result reverse(
      Boolean b,
      Character c,
      Short s,
      Integer i,
      Long l,
      Float f,
      Double d,
      UUID uuid,
      OptionalInt oi,
      OptionalLong ol,
      OptionalDouble od,
      String str,
      Optional<String> ostr) {
    return ok();
  }

  public Result interpolatorWarning(String parameter) {
    return ok(parameter);
  }
}
