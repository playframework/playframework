/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.server;

import javax.net.ssl.SSLEngine;

public interface SSLEngineProvider {

    /**
     * @return the SSL engine to be used for HTTPS connection.
     */
    SSLEngine createSSLEngine();

}
