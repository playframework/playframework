/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

/**
 * A binding target.
 *
 * This abstract class captures the four possible types of targets.
 *
 * See the {@link Module} class for information on how to provide bindings.
 */
public abstract class BindingTarget<T> {
    BindingTarget() {
    }

    public abstract play.api.inject.BindingTarget<T> asScala();
}
