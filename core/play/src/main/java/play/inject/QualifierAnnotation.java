/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

/**
 * A qualifier annotation.
 *
 * <p>Since bindings may specify either annotations, or instances of annotations, this abstraction
 * captures either of those two possibilities.
 *
 * <p>See the {@link Module} class for information on how to provide bindings.
 */
public abstract class QualifierAnnotation {
  QualifierAnnotation() {}

  public abstract play.api.inject.QualifierAnnotation asScala();
}
