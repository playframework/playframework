/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import play.libs.ws.WSClient;

/**
 * Java WSClient components.
 */
public interface WSClientComponents {
    WSClient wsClient();
}
