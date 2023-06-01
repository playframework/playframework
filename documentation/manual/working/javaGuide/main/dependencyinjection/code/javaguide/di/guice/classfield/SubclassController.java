/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.classfield;

// #class-field-dependency-injection
import javax.inject.Singleton;
import play.mvc.Result;

@Singleton
public class SubclassController extends BaseController {
  public Result index() {
    return someBaseAction("index");
  }
}
// #class-field-dependency-injection
