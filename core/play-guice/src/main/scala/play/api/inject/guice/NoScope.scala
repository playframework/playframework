/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject.guice

import java.lang.annotation.Annotation
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import jakarta.inject.Scope

/**
 * Custom scope annotation to represent the absence of any specific scope.
 *
 * This annotation is created because Play Framework's GuiceInjectorBuilder
 * requires a scope to be defined with an annotation. Since Guice's NO_SCOPE
 * is not bound to an annotation by default, we define @NoScope to represent
 * a "no-scope" condition. It will be detected by the builder to apply the
 * desired behavior for unscoped bindings.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
@Target(Array(ElementType.TYPE, ElementType.METHOD, ElementType.FIELD))
class NoScope extends Annotation {
  override def annotationType(): Class[? <: Annotation] = classOf[NoScope]
}
