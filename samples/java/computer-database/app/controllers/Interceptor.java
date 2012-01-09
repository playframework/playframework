package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import models.Computer;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;

public class Interceptor extends Action.Simple {

  @With(Interceptor.class)
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CommonHeader {
  }

  public Result call(Http.Context ctx) throws Throwable {
    // This is a simple operation which could be repeated in every controller.
    // However, for demonstration purposes we do it here to show how to avoid
    // repeating a more complicated operation in each controller.
    int rowCount = Computer.find.findRowCount();
    return delegate.call(new ComputerContext(
        ctx.request(), ctx.session(), ctx.flash(), rowCount));
  }

  /**
   * This is the only way to pass info to the request.
   * Play 2 does not have the equivalent of Play 1's request.args.
   */
  public static class ComputerContext extends Http.Context {

    private final int numComputers;
    
    public ComputerContext(
        Request request,
        Map<String, String> sessionData,
        Map<String, String> flashData,
        int numComputers) {
      super(request, sessionData, flashData);
      this.numComputers =numComputers;
    }
    
    public int numComputers() {
      return numComputers;
    }
    
  }
  
}
