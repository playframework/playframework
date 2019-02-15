/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import java.lang.annotation.Annotation;

/**
 * A qualifier annotation instance.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public final class QualifierInstance<T extends Annotation> extends QualifierAnnotation {
    private final play.api.inject.QualifierInstance<T> underlying;

    public QualifierInstance(final T instance) {
        this(play.api.inject.QualifierInstance.apply(instance));
    }

    public QualifierInstance(final play.api.inject.QualifierInstance<T> underlying) {
        super();
        this.underlying = underlying;
    }

    public T getInstance() {
        return underlying.instance();
    }

    @Override
    public play.api.inject.QualifierInstance asScala() {
        return underlying;
    }
}
