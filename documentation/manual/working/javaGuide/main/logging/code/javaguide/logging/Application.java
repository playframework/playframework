/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.logging;

// #logging-pattern-mix
import play.Logger;
import play.Logger.ALogger;
import play.mvc.Action;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import java.util.concurrent.CompletionStage;

public class Application extends Controller {

  private static final ALogger logger = Logger.of(Application.class);

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

  private ALogger accessLogger = Logger.of("access");

  public CompletionStage<Result> call(Http.Context ctx) {
    final Request request = ctx.request();
    accessLogger.info(
        "method={} uri={} remote-address={}",
        request.method(),
        request.uri(),
        request.remoteAddress());

    return delegate.call(ctx);
  }
}
// #logging-pattern-mix
