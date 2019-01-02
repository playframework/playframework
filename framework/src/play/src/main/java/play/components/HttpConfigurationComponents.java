/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.api.http.HttpConfiguration;
import play.api.http.SessionConfiguration;

/**
 * Http Configuration Java Components.
 */
public interface HttpConfigurationComponents {

    HttpConfiguration httpConfiguration();

    /**
     * @return the session configuration from the {@link #httpConfiguration()}.
     */
    default SessionConfiguration sessionConfiguration() {
        return httpConfiguration().session();
    }
}
