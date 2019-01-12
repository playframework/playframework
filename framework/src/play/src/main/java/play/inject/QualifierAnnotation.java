/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

/**
 * A qualifier annotation.
 *
 * Since bindings may specify either annotations, or instances of annotations, this abstraction captures either of
 * those two possibilities.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public abstract class QualifierAnnotation {
    QualifierAnnotation() {
    }

    public abstract play.api.inject.QualifierAnnotation asScala();
}
