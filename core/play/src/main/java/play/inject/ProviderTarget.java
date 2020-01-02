/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import javax.inject.Provider;

/**
 * A binding target that is provided by a provider instance.
 *
 * <p>See the {@link Module} class for information on how to provide bindings.
 */
public final class ProviderTarget<T> extends BindingTarget<T> {
  private final play.api.inject.ProviderTarget<T> underlying;

  public ProviderTarget(final Provider<? extends T> provider) {
    this(play.api.inject.ProviderTarget.apply(provider));
  }

  public ProviderTarget(final play.api.inject.ProviderTarget<T> underlying) {
    super();
    this.underlying = underlying;
  }

  public Provider<? extends T> getProvider() {
    return underlying.provider();
  }

  @Override
  public play.api.inject.ProviderTarget<T> asScala() {
    return underlying;
  }
}
