/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

@Singleton
public class DelegateApplicationLifecycle implements ApplicationLifecycle {
  private final play.api.inject.ApplicationLifecycle delegate;

  @Inject
  public DelegateApplicationLifecycle(play.api.inject.ApplicationLifecycle delegate) {
    this.delegate = delegate;
  }

  @Override
  public void addStopHook(final Callable<? extends CompletionStage<?>> hook) {
    delegate.addStopHook(hook);
  }

  @Override
  public play.api.inject.ApplicationLifecycle asScala() {
    return delegate;
  }
}
