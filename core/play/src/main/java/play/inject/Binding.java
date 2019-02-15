/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import scala.compat.java8.OptionConverters;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * A binding.
 *
 * Bindings are used to bind classes, optionally qualified by a JSR-330 qualifier annotation, to instances, providers or
 * implementation classes.
 *
 * Bindings may also specify a JSR-330 scope.  If, and only if that scope is
 * <a href="http://docs.oracle.com/javase/8/docs/api/javax/inject/Singleton">javax.inject.Singleton</a>, then the
 * binding may declare itself to be eagerly instantiated.  In which case, it should be eagerly instantiated when Play
 * starts up.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public final class Binding<T> {
    private final play.api.inject.Binding<T> underlying;

    /**
     * @param key The binding key.
     * @param target The binding target.
     * @param scope The JSR-330 scope.
     * @param eager Whether the binding should be eagerly instantiated.
     * @param source Where this object was bound. Used in error reporting.
     */
    public Binding(final BindingKey<T> key, final Optional<BindingTarget<T>> target,
            final Optional<Class<? extends Annotation>> scope, final Boolean eager, final Object source) {
        this(play.api.inject.Binding.apply(key.asScala(), OptionConverters.toScala(target.map(BindingTarget::asScala)),
            OptionConverters.toScala(scope), eager, source));
    }

    public Binding(final play.api.inject.Binding<T> underlying) {
        this.underlying = underlying;
    }

    public BindingKey<T> getKey() {
        return underlying.key().asJava();
    }

    public Optional<BindingTarget<T>> getTarget() {
        return OptionConverters.toJava(underlying.target()).map(play.api.inject.BindingTarget::asJava);
    }

    public Optional<Class<? extends Annotation>> getScope() {
        return OptionConverters.toJava(underlying.scope());
    }

    public Boolean getEager() {
        return underlying.eager();
    }

    public Object getSource() {
        return underlying.source();
    }

    /**
     * Configure the scope for this binding.
     */
    public <A extends Annotation> Binding<T> in(final Class<A> scope) {
        return underlying.in(scope).asJava();
    }

    /**
     * Eagerly instantiate this binding when Play starts up.
     */
    public Binding<T> eagerly() {
        return underlying.eagerly().asJava();
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    public play.api.inject.Binding<T> asScala() {
        return underlying;
    }
}
