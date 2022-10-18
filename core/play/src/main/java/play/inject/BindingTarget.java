/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

/**
 * A binding target.
 *
 * <p>This abstract class captures the four possible types of targets.
 *
 * <p>See the {@link Module} class for information on how to provide bindings.
 */
public abstract class BindingTarget<T> {
  BindingTarget() {}

  public abstract play.api.inject.BindingTarget<T> asScala();
}
