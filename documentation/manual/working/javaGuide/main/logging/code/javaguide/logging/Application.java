/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.logging;

// #logging-pattern-mix
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Action;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

public class Application extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  @With(AccessLoggingAction.class)
  public Result index() {
    try {
      final int result = riskyCalculation();
      return ok("Result=" + result);
    } catch (Throwable t) {
      logger.error("Exception with riskyCalculation", t);
      return internalServerError("Error in calculation: " + t.getMessage());
    }
  }

  private static int riskyCalculation() {
    return 10 / (new java.util.Random()).nextInt(2);
  }
}

class AccessLoggingAction extends Action.Simple {

  private static final Logger accessLogger = LoggerFactory.getLogger(AccessLoggingAction.class);

  public CompletionStage<Result> call(Http.Request request) {
    accessLogger.info(
        "method={} uri={} remote-address={}",
        request.method(),
        request.uri(),
        request.remoteAddress());

    return delegate.call(request);
  }
}
// #logging-pattern-mix
