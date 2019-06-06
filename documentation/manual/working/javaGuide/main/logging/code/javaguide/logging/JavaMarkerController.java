/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.logging;

import org.slf4j.Marker;
import play.Logger;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static net.logstash.logback.marker.Markers.append;

public class JavaMarkerController extends Controller {
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public JavaMarkerController(HttpExecutionContext httpExecutionContext) {
    this.httpExecutionContext = httpExecutionContext;
  }

  private final Logger.ALogger logger = Logger.of(this.getClass());

  // #logging-log-info-with-request-context
  // ###insert: import static net.logstash.logback.marker.Markers.append;

  private Marker requestMarker() {
    Http.Request request = Http.Context.current().request();
    return append("host", request.host()).and(append("path", request.path()));
  }

  public Result index() {
    logger.info(requestMarker(), "Rendering index()");
    return ok("foo");
  }
  // #logging-log-info-with-request-context

  // #logging-log-info-with-async-request-context
  public CompletionStage<Result> asyncIndex() {
    return CompletableFuture.supplyAsync(
        () -> {
          logger.info(requestMarker(), "Rendering asyncIndex()");
          return ok("foo");
        },
        httpExecutionContext.current());
  }
  // #logging-log-info-with-async-request-context
}
