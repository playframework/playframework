/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.inject.Inject;
import play.inject.Injector;

/** Creates validator instances with injections available. */
public class DefaultConstraintValidatorFactory implements ConstraintValidatorFactory {

  private Injector injector;

  @Inject
  public DefaultConstraintValidatorFactory(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
    return this.injector.instanceOf(key);
  }

  @Override
  public void releaseInstance(final ConstraintValidator<?, ?> instance) {
    // Garbage collector will do it
  }
}
