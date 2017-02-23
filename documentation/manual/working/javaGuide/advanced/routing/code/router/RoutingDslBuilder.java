package router;

import play.routing.Router;
import play.mvc.Controller;
import play.routing.RoutingDsl;

public class RoutingDslBuilder extends Controller{

  public static Router getRouter() {
    return new RoutingDsl()
      .GET("/hello/:to").routeTo(to -> ok("Hello " + to))
      .build();
  }
}

