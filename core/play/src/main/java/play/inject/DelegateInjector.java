/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import play.api.inject.BindingKey;

@Singleton
public class DelegateInjector implements Injector {
  public final play.api.inject.Injector injector;

  @Inject
  public DelegateInjector(play.api.inject.Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> T instanceOf(Class<T> clazz) {
    return injector.instanceOf(clazz);
  }

  @Override
  public <T> T instanceOf(BindingKey<T> key) {
    return injector.instanceOf(key);
  }

  @Override
  public play.api.inject.Injector asScala() {
    return injector;
  }
}
