/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import scala.compat.java8.functionConverterImpls.FromJavaSupplier;
import scala.compat.java8.OptionConverters;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A binding key.
 *
 * A binding key consists of a class and zero or more JSR-330 qualifiers.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public final class BindingKey<T> {
    private final play.api.inject.BindingKey<T> underlying;

    /**
     * A binding key.
     *
     * A binding key consists of a class and zero or more JSR-330 qualifiers.
     *
     * See the {@link Module} class for information on how to provide bindings.
     *
     * @param clazz The class to bind.
     * @param qualifier An optional qualifier.
     */
    public BindingKey(final Class<T> clazz, final Optional<QualifierAnnotation> qualifier) {
        this(play.api.inject.BindingKey.apply(clazz, OptionConverters.toScala(qualifier.map(QualifierAnnotation::asScala))));
    }

    public BindingKey(final play.api.inject.BindingKey<T> underlying) {
        this.underlying = underlying;
    }

    public BindingKey(final Class<T> clazz) {
        this(clazz, Optional.empty());
    }

    public Class<T> getClazz() {
        return underlying.clazz();
    }

    public Optional<QualifierAnnotation> getQualifier() {
        return OptionConverters.toJava(underlying.qualifier()).map(play.api.inject.QualifierAnnotation::asJava);
    }

    /**
     * Qualify this binding key with the given instance of an annotation.
     *
     * This can be used to specify bindings with annotations that have particular values.
     */
    public <A extends Annotation> BindingKey<T> qualifiedWith(final A instance) {
        return underlying.qualifiedWith(instance).asJava();
    }

    /**
     * Qualify this binding key with the given annotation.
     *
     * For example, you may have both a cached implementation, and a direct implementation of a service. To differentiate
     * between them, you may define a Cached annotation:
     *
     * <pre>
     * {@code
     *   bindClass(Foo.class).qualifiedWith(Cached.class).to(FooCached.class),
     *   bindClass(Foo.class).to(FooImpl.class)
     *
     *   ...
     *
     *   class MyController {
     *     {@literal @}Inject
     *     MyController({@literal @}Cached Foo foo) {
     *       ...
     *     }
     *     ...
     *   }
     * }
     * </pre>
     *
     * In the above example, the controller will get the cached {@code Foo} service.
     */
    public <A extends Annotation> BindingKey<T> qualifiedWith(final Class<A> annotation) {
        return underlying.qualifiedWith(annotation).asJava();
    }

    /**
     * Qualify this binding key with the given name.
     *
     * For example, you may have both a cached implementation, and a direct implementation of a service. To differentiate
     * between them, you may decide to name the cached one:
     *
     * <pre>
     * {@code
     *   bindClass(Foo.class).qualifiedWith("cached").to(FooCached.class),
     *   bindClass(Foo.class).to(FooImpl.class)
     *
     *   ...
     *
     *   class MyController {
     *     {@literal @}Inject
     *     MyController({@literal @}Named("cached") Foo foo) {
     *       ...
     *     }
     *     ...
     *   }
     * }
     * </pre>
     *
     * In the above example, the controller will get the cached `Foo` service.
     */
    public BindingKey<T> qualifiedWith(final String name) {
        return underlying.qualifiedWith(name).asJava();
    }

    /**
     * Bind this binding key to the given implementation class.
     *
     * This class will be instantiated and injected by the injection framework.
     */
    public Binding<T> to(final Class<? extends T> implementation) {
        return underlying.to(implementation).asJava();
    }

    /**
     * Bind this binding key to the given provider instance.
     *
     * This provider instance will be invoked to obtain the implementation for the key.
     */
    public Binding<T> to(final Provider<? extends T> provider) {
        return underlying.to(provider).asJava();
    }

    /**
     * Bind this binding key to the given instance.
     */
    public <A extends T> Binding<T> to(final Supplier<A> instance) {
        return underlying.to(new FromJavaSupplier<>(instance)).asJava();
    }

    /**
     * Bind this binding key to another binding key.
     */
    public Binding<T> to(final BindingKey<? extends T> key) {
        return underlying.to(key.asScala()).asJava();
    }

    /**
     * Bind this binding key to the given provider class.
     *
     * The dependency injection framework will instantiate and inject this provider, and then invoke its `get` method
     * whenever an instance of the class is needed.
     */
    public <P extends Provider<? extends T>> Binding<T> toProvider(final Class<P> provider) {
        return underlying.toProvider(provider).asJava();
    }

    /**
     * Bind this binding key to the given instance.
     */
    public Binding<T> toInstance(final T instance) {
        return underlying.toInstance(instance).asJava();
    }

    /**
     * Bind this binding key to itself.
     */
    public Binding<T> toSelf() {
        return underlying.toSelf().asJava();
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    public play.api.inject.BindingKey<T> asScala() {
        return underlying;
    }
}
