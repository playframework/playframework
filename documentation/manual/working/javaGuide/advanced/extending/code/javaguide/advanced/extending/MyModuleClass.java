/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.extending;

//#module-decl
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;

import scala.collection.Seq;

public class MyModuleClass extends Module {
  public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
    return seq(
      bind(MyComponent.class).to(MyComponentImpl.class)
    );
  }
}
//#module-decl
