/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.hello;

import controllers.AssetsFinder;
import play.mvc.*;

import javax.inject.Inject;

public class HelloController extends Controller {

  @Inject
  public HelloController() {}

  // #hello-world-index-action
  public Result index() {
<<<<<<< HEAD
<<<<<<< HEAD
    // ###replace:        return ok(views.html.index.render("Your new application is
    // ready."));
    return ok(javaguide.hello.html.index.render("Your new application is ready."));
=======
    // ###replace: return ok(views.html.index.render("Your new application is ready.", assetsFinder));
=======
// ###replace: return ok(views.html.index.render("Your new application is ready.", assetsFinder));
>>>>>>> 6a56b9b225 (moved comment left)
    return ok(javaguide.hello.html.index.render("Your new application is ready.", assetsFinder));
>>>>>>> e3c37f9ed1 (Fixed `###replace:` tag that led to incorrect docs)
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
