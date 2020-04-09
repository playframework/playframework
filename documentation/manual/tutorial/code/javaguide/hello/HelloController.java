/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.hello;

import javax.inject.Inject;
import play.mvc.*;

public class HelloController extends Controller {

  @Inject
  public HelloController() {}

  // #hello-world-index-action
  public Result index() {
    // ###replace: return ok(views.html.index.render("Your app is ready."));
    return ok(javaguide.hello.html.index.render("Your app is ready."));
  }
  // #hello-world-index-action

  // #hello-world-hello-action
  public Result hello() {
    // ###replace:     return ok(views.html.hello.render());
    return ok(javaguide.hello.html.hello.render());
  }
  // #hello-world-hello-action

  /*
  //#hello-world-hello-error-action
  public Result hello(String name) {
      return ok(views.html.hello.render());
  }
  //#hello-world-hello-error-action
   */

  // #hello-world-hello-correct-action
  public Result hello(String name) {
    // ###replace:    return ok(views.html.hello.render(name));
    return ok(javaguide.hello.html.helloName.render(name));
  }
  // #hello-world-hello-correct-action
}
