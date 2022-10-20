/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
