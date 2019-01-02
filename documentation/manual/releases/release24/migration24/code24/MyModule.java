/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

//#module-decl
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;

import scala.collection.Seq;

public class MyModule extends Module {
  public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
    return seq(
      bind(MyComponent.class).to(MyComponentImpl.class)
    );
  }
}
//#module-decl
