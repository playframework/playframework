/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import javax.inject.Inject;
import javax.inject.Singleton;
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
