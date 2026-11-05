/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import jakarta.inject.Scope;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects unscoped provisioning for a Play binding translated by Guice.
 *
 * <p>Use {@code .in(NoScope.class)} in Java or {@code .in[NoScope]} in Scala. This annotation
 * cannot be applied to classes or methods.
 *
 * <p>For a self binding, this overrides the type's scope annotation. For a binding to an
 * implementation class, Play constructs the implementation directly each time Guice supplies an
 * instance for that binding, without consulting bindings or overrides for the implementation key.
 * The implementation needs an injectable constructor, and its separate binding keeps its own scope.
 *
 * <p>Constructor selection respects the Scala builder's {@code requireAtInjectOnConstructors()}
 * option. An {@code @Inject} requirement enabled only by a native Guice module is not automatically
 * enforced for these explicit constructor bindings.
 *
 * <p>For provider and binding-key targets, provisioning behaves like an omitted scope. The provider
 * or target binding may still return the same instance.
 */
@Scope
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoScope {}
