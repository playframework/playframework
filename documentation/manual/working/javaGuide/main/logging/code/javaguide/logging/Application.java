package javaguide.logging;

//#logging-pattern-mix
import play.Logger;
import play.Logger.ALogger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;

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
  
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {
    final Request request = ctx.request();
    accessLogger.info("method=" + request.method() + " uri=" + request.uri() + " remote-address=" + request.remoteAddress());
    
    return delegate.call(ctx);
  }
}
//#logging-pattern-mix