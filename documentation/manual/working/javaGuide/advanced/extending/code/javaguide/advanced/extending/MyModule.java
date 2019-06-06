/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.extending;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import scala.collection.Seq;

// #module-class-definition
public class MyModule extends play.api.inject.Module {
  public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
    return seq(bind(MyApi.class).toSelf());
  }
}
// #module-class-definition
