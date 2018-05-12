/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import play.api._
import play.api.libs.crypto._

// Keep Crypto around to manage global state for now...
private[play] object Crypto {

  private val cookieSignerCache: (Application) => CookieSigner = Application.instanceCache[CookieSigner]

  // Temporary placeholder until we can move out Session / Cookie singleton objects
  def cookieSigner: CookieSigner = {
    Play.privateMaybeApplication.fold {
      sys.error("The global cookie signer instance requires a running application!")
    }(cookieSignerCache)
  }

}
