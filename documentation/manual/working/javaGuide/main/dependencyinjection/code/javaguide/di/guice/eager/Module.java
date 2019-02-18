/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.eager;

import javaguide.di.*;

// #eager-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

// A Module is needed to register bindings
public class Module extends AbstractModule {
  protected void configure() {

    // Bind the `Hello` interface to the `EnglishHello` implementation as eager singleton.
    bind(Hello.class).annotatedWith(Names.named("en")).to(EnglishHello.class).asEagerSingleton();

    bind(Hello.class).annotatedWith(Names.named("de")).to(GermanHello.class).asEagerSingleton();
  }
}
// #eager-guice-module
