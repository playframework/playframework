/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
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

  public Result multiParams(
      String a,
      String b,
      String c,
      String d,
      String e,
      String f,
      String g,
      String h,
      String i,
      String j,
      String k,
      String l,
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
        a + b + c + d + e + f + g + h + i + j + k + l + m + n + o + p + q + r + s + t + u + v + w
            + x + y + z);
  }

  public Result urlcoding(String dynamic, String _static, String query) {
    return ok(String.format("dynamic=%s static=%s query=%s", dynamic, _static, query));
  }

  public Result keyword(String keyword) {
    return ok(keyword);
  }

  public Result keywordDefault(String keyword) {
    return ok(keyword);
  }

  public Result keywordPath(String keyword) {
    return ok(keyword);
  }

  public Result interpolatorWarning(String parameter) {
    return ok(parameter);
  }
}
