/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

/**
 * A binding target that is provided by a class.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public final class ConstructionTarget<T> extends BindingTarget<T> {
    private final play.api.inject.ConstructionTarget<T> underlying;

    public ConstructionTarget(final Class<? extends T> implementation) {
        this(play.api.inject.ConstructionTarget.apply(implementation));
    }

    public ConstructionTarget(final play.api.inject.ConstructionTarget<T> underlying) {
        super();
        this.underlying = underlying;
    }

    public Class<? extends T> getImplementation() {
        return underlying.implementation();
    }

    @Override
    public play.api.inject.ConstructionTarget<T> asScala() {
        return underlying;
    }
}
