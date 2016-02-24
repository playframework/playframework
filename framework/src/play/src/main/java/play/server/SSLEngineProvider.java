/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.server;

import javax.net.ssl.SSLEngine;

public interface SSLEngineProvider {

    /**
     * @return the SSL engine to be used for HTTPS connection.
     */
    SSLEngine createSSLEngine();

}
