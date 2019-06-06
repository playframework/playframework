/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice;

import javaguide.di.*;

// #guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class Module extends AbstractModule {
  protected void configure() {

    bind(Hello.class).annotatedWith(Names.named("en")).to(EnglishHello.class);

    bind(Hello.class).annotatedWith(Names.named("de")).to(GermanHello.class);
  }
}
// #guice-module
