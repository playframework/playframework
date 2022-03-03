/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play;

/**
 * This helper class provides all the built-in component dependencies by trading them for a single
 * dependency - the {@linkplain #context() application loader context}.
 */
public abstract class BuiltInComponentsFromContext extends ContextBasedBuiltInComponents {

  private final ApplicationLoader.Context context;

  public BuiltInComponentsFromContext(ApplicationLoader.Context context) {
    this.context = context;
  }

  @Override
  public ApplicationLoader.Context context() {
    return this.context;
  }
}
