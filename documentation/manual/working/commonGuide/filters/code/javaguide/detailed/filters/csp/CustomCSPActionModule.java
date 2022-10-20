/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import com.google.inject.AbstractModule;

// #java-csp-module
public class CustomCSPActionModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MyDynamicCSPAction.class).asEagerSingleton();
    bind(AssetCache.class).asEagerSingleton();
  }
}
// #java-csp-module
