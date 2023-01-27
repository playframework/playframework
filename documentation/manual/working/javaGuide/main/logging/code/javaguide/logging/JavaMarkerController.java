/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.logging;

import static net.logstash.logback.marker.Markers.append;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class JavaMarkerController extends Controller {
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public JavaMarkerController(HttpExecutionContext httpExecutionContext) {
    this.httpExecutionContext = httpExecutionContext;
  }

  private static final Logger logger = LoggerFactory.getLogger(JavaMarkerController.class);

  // #logging-log-info-with-request-context
  // ###insert: import static net.logstash.logback.marker.Markers.append;

  private Marker requestMarker(Http.Request request) {
    return append("host", request.host()).and(append("path", request.path()));
  }

  public Result index(Http.Request request) {
    logger.info(requestMarker(request), "Rendering index()");
    return ok("foo");
  }
  // #logging-log-info-with-request-context
}
