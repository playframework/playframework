/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.logging;

import javax.inject.Singleton;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Singleton
// #logging-log-trace-with-tracer-controller
public class JavaTracerController extends Controller {

  private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

  private static final Marker tracerMarker = org.slf4j.MarkerFactory.getMarker("TRACER");

  private Marker tracer(Http.Request request) {
    Marker marker = MarkerFactory.getDetachedMarker("dynamic"); // base do-nothing marker...
    request.queryString("trace").ifPresent(s -> marker.add(tracerMarker));
    return marker;
  }

  public Result index(Http.Request request) {
    logger.trace(tracer(request), "Only logged if queryString contains trace=true");
    return ok("hello world");
  }
}
// #logging-log-trace-with-tracer-controller
