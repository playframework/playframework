/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import java.lang.annotation.Annotation;

/**
 * A qualifier annotation instance.
 *
 * <p>See the {@link Module} class for information on how to provide bindings.
 */
public final class QualifierClass<T extends Annotation> extends QualifierAnnotation {
  private final play.api.inject.QualifierClass<T> underlying;

  public QualifierClass(final Class<T> clazz) {
    this(play.api.inject.QualifierClass.apply(clazz));
  }

  public QualifierClass(final play.api.inject.QualifierClass<T> underlying) {
    super();
    this.underlying = underlying;
  }

  public Class<T> getClazz() {
    return underlying.clazz();
  }

  @Override
  public play.api.inject.QualifierClass asScala() {
    return underlying;
  }
}
