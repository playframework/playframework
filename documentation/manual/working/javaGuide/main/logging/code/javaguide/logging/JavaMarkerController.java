/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
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

    private static final Logger logger = LoggerFactory.getLogger(JavaMarkerController.class);

    // #logging-log-info-with-request-context
    // ###insert: import static net.logstash.logback.marker.Markers.append;

    private Marker requestMarker(Http.Request request) {
        return append("host", request.host())
                .and(append("path", request.path()));
    }

    public Result index(Http.Request request) {
        logger.info(requestMarker(request), "Rendering index()");
        return ok("foo");
    }
    // #logging-log-info-with-request-context

    // #logging-log-info-with-async-request-context
    public CompletionStage<Result> asyncIndex(Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info(requestMarker(request), "Rendering asyncIndex()");
            return ok("foo");
        }, httpExecutionContext.current());
    }
    // #logging-log-info-with-async-request-context
}
