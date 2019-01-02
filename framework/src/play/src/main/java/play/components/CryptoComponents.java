/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;

import java.time.Clock;

public interface CryptoComponents {

    CookieSigner cookieSigner();

    CSRFTokenSigner csrfTokenSigner();

    // TODO Should this be part of the interface?
    default Clock clock() {
        return Clock.systemUTC();
    }
}
