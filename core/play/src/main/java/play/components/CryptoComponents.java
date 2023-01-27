/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import java.time.Clock;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;

public interface CryptoComponents {

  CookieSigner cookieSigner();

  CSRFTokenSigner csrfTokenSigner();

  // TODO Should this be part of the interface?
  default Clock clock() {
    return Clock.systemUTC();
  }
}
