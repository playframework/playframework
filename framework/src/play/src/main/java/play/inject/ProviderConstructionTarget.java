/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import javax.inject.Provider;

/**
 * A binding target that is provided by a provider class.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public final class ProviderConstructionTarget<T> extends BindingTarget<T> {
    private final play.api.inject.ProviderConstructionTarget<T> underlying;

    public ProviderConstructionTarget(final Class<? extends Provider<? extends T>> provider) {
        this(play.api.inject.ProviderConstructionTarget.apply(provider));
    }

    public ProviderConstructionTarget(final play.api.inject.ProviderConstructionTarget<T> underlying) {
        super();
        this.underlying = underlying;
    }

    public Class<? extends Provider<? extends T>> getProvider() {
        return underlying.provider();
    }

    @Override
    public play.api.inject.ProviderConstructionTarget<T> asScala() {
        return underlying;
    }
}
