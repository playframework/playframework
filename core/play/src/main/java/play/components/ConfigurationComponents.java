/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import com.typesafe.config.Config;
import play.api.Configuration;

/**
 * Provides configuration components.
 *
 * @see Config
 * @see Configuration
 */
public interface ConfigurationComponents {

    Config config();

    default Configuration configuration() {
        return new Configuration(config());
    }
}
