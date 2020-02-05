/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.hello;

import controllers.AssetsFinder;
import play.mvc.*;

import javax.inject.Inject;

public class HelloController extends Controller {

  private final AssetsFinder assetsFinder;

  @Inject
  public HelloController(AssetsFinder assetsFinder) {
    this.assetsFinder = assetsFinder;
  }

  // #hello-world-index-action
  public Result index() {
    // ###replace:        return ok(views.html.index.render("Your new application is
    // ready.",assetsFinder));
    return ok(javaguide.hello.html.index.render("Your new application is ready.", assetsFinder));
  }
  // #hello-world-index-action

  // #hello-world-hello-action
  public Result hello() {
    // ###replace:     return ok(views.html.hello.render(assetsFinder));
    return ok(javaguide.hello.html.hello.render(assetsFinder));
  }
  // #hello-world-hello-action

  /*
  //#hello-world-hello-error-action
  public Result hello(String name) {
      return ok(views.html.hello.render(assetsFinder));
  }
  //#hello-world-hello-error-action
   */

  // #hello-world-hello-correct-action
  public Result hello(String name) {
    // ###replace:    return ok(views.html.hello.render(name, assetsFinder));
    return ok(javaguide.hello.html.helloName.render(name, assetsFinder));
  }
  // #hello-world-hello-correct-action
}
