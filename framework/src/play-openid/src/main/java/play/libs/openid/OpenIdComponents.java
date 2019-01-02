/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import play.api.libs.openid.Discovery;
import play.api.libs.openid.WsDiscovery;
import play.api.libs.openid.WsOpenIdClient;
import play.components.AkkaComponents;
import play.libs.ws.ahc.WSClientComponents;

/**
 * OpenID Java components.
 */
public interface OpenIdComponents extends WSClientComponents, AkkaComponents {

    default Discovery openIdDiscovery() {
        return new WsDiscovery(wsClient().asScala(), executionContext());
    }

    default OpenIdClient openIdClient() {
        return new DefaultOpenIdClient(
            new WsOpenIdClient(
                wsClient().asScala(),
                openIdDiscovery(),
                executionContext()
            ),
            executionContext()
        );
    }
}
