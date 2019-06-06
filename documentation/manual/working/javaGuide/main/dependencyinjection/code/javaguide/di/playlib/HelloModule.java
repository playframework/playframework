/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.playlib;

import javaguide.di.*;

// #play-module
import play.api.*;
import play.api.inject.*;
import scala.collection.Seq;

public class HelloModule extends Module {
  @Override
  public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
    return seq(
        bind(Hello.class).qualifiedWith("en").to(EnglishHello.class),
        bind(Hello.class).qualifiedWith("de").to(GermanHello.class));
  }
}
// #play-module
