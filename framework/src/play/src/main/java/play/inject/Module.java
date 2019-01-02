/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import com.typesafe.config.Config;
import play.Environment;
import scala.collection.JavaConverters;
import scala.collection.immutable.Seq;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A Play dependency injection module.
 *
 * Dependency injection modules can be used by Play plugins to provide bindings for JSR-330 compliant
 * ApplicationLoaders.  Any plugin that wants to provide components that a Play application can use may implement
 * one of these.
 *
 * Providing custom modules can be done by appending their fully qualified class names to `play.modules.enabled` in
 * `application.conf`, for example
 *
 * <pre> <code>
 * play.modules.enabled += "com.example.FooModule"
 * play.modules.enabled += "com.example.BarModule"
 * </code> </pre>
 *
 * It is strongly advised that in addition to providing a module for JSR-330 DI, that plugins also provide a Scala
 * trait that constructs the modules manually.  This allows for use of the module without needing a runtime dependency
 * injection provider.
 *
 * The `bind` methods are provided only as a DSL for specifying bindings. For example:
 *
 * <pre> <code>
 * {@literal @}Override
 * public List&lt;Binding&lt;?&gt;&gt; bindings(Environment environment, Config config) {
 *     return Arrays.asList(
 *         bindClass(Foo.class).to(FooImpl.class),
 *         bindClass(Bar.class).to(() -&gt; new Bar()),
 *         bindClass(Foo.class).qualifiedWith(SomeQualifier.class).to(OtherFoo.class)
 *     );
 * }
 * </code> </pre>
 */
public abstract class Module extends play.api.inject.Module {
    public abstract List<Binding<?>> bindings(final Environment environment, final Config config);

    @Override
    public final Seq<play.api.inject.Binding<?>> bindings(final play.api.Environment environment,
            final play.api.Configuration configuration) {
        List<play.api.inject.Binding<?>> list = bindings(environment.asJava(), configuration.underlying()).stream()
            .map(Binding::asScala)
            .collect(Collectors.toList());
        return JavaConverters.collectionAsScalaIterableConverter(list).asScala().toList();
    }

    /**
     * Create a binding key for the given class.
     */
    public static <T> BindingKey<T> bindClass(final Class<T> clazz) {
        return new BindingKey<>(clazz);
    }
}
