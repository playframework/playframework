/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.classfield;

// #class-field-dependency-injection
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public class BaseController extends Controller {
  // LiveCounter will be injected
  @Inject protected volatile Counter counter = new NoopCounter();

  public Result someBaseAction(String source) {
    counter.inc(source);
    return ok(source);
  }
}
// #class-field-dependency-injection
