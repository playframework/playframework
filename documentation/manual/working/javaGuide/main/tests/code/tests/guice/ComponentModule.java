/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests.guice;

// #component-module
import com.google.inject.AbstractModule;

public class ComponentModule extends AbstractModule {
  protected void configure() {
    bind(Component.class).to(DefaultComponent.class);
  }
}
// #component-module
