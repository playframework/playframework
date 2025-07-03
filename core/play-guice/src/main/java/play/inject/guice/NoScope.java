/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import jakarta.inject.Scope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom scope annotation to represent the absence of any specific scope.
 *
 * <p>This annotation is created because Play Framework's GuiceInjectorBuilder requires a scope to
 * be defined with an annotation. Since Guice's NO_SCOPE is not bound to an annotation by default,
 * we define @NoScope to represent a "no-scope" condition. It will be detected by the builder to
 * apply the desired behavior for unscoped bindings.
 */
@Scope
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoScope {}
