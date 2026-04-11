/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.api.http.HttpConfiguration;
import play.api.http.SessionConfiguration;

/** Http Configuration Java Components. */
public interface HttpConfigurationComponents {

  HttpConfiguration httpConfiguration();

  /**
   * @return the session configuration from the {@link #httpConfiguration()}.
   */
  default SessionConfiguration sessionConfiguration() {
    return httpConfiguration().session();
  }
}
