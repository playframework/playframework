/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import java.util.Optional;
import play.Environment;
import play.core.SourceMapper;
import play.inject.ApplicationLifecycle;
import play.routing.Router;

public interface BaseComponents extends ConfigurationComponents {

  /**
   * The application environment.
   *
   * @return an instance of the application environment
   */
  Environment environment();

  Optional<SourceMapper> sourceMapper();

  ApplicationLifecycle applicationLifecycle();

  Router router();
}
