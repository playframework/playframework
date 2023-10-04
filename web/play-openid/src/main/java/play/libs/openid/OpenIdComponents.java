/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import play.api.libs.openid.Discovery;
import play.api.libs.openid.WsDiscovery;
import play.api.libs.openid.WsOpenIdClient;
import play.components.PekkoComponents;
import play.libs.ws.ahc.WSClientComponents;

/** OpenID Java components. */
public interface OpenIdComponents extends WSClientComponents, PekkoComponents {

  default Discovery openIdDiscovery() {
    return new WsDiscovery(wsClient().asScala(), executionContext());
  }

  default OpenIdClient openIdClient() {
    return new DefaultOpenIdClient(
        new WsOpenIdClient(wsClient().asScala(), openIdDiscovery(), executionContext()),
        executionContext());
  }
}
