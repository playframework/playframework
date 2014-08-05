/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.ebean;

import com.avaje.ebean.config.ServerConfig;
import java.util.Map;

public interface EbeanConfig {

    public String defaultServer();

    public Map<String, ServerConfig> serverConfigs();

}
