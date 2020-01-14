/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.hello;

<<<<<<< HEAD
import javax.inject.Inject;
=======
import controllers.AssetsFinder;
>>>>>>> b68644d329 (Final changes after merging the code in samples)
import play.mvc.*;

import javax.inject.Inject;

public class HelloController extends Controller {

<<<<<<< HEAD
  @Inject
  public HelloController() {}

  // #hello-world-index-action
  public Result index() {
    // ###replace: return ok(views.html.index.render("Your app is ready."));
    return ok(javaguide.hello.html.index.render("Your app is ready."));
=======
  private final AssetsFinder assetsFinder;

  @Inject
  public HelloController(AssetsFinder assetsFinder) {
    this.assetsFinder = assetsFinder;
  }

  // #hello-world-index-action
  public Result index() {
<<<<<<< HEAD
    return ok(
        // ###replace:        views.html.index.render(
        javaguide.hello.html.index.render("Your new application is ready.", assetsFinder));
>>>>>>> 145091904b (Reviews the java version of the starter tutorial)
=======
    // ###replace:        return ok(views.html.index.render("Your new application is
    // ready.",assetsFinder));
    return ok(javaguide.hello.html.index.render("Your new application is ready.", assetsFinder));
>>>>>>> b68644d329 (Final changes after merging the code in samples)
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
