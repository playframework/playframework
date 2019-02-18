/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.eager;

import javaguide.di.*;

// #eager-guice-module
import com.google.inject.AbstractModule;

public class StartModule extends AbstractModule {
  protected void configure() {
    bind(ApplicationStart.class).asEagerSingleton();
  }
}
// #eager-guice-module
