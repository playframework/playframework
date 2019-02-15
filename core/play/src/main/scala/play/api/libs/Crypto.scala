/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import play.api._
import play.api.libs.crypto._

import scala.util.{ Failure, Success }

// Keep Crypto around to manage global state for now...
@deprecated("Access global state. Inject a CookieSigner instead", "2.7.0")
private[play] object Crypto {

  private val cookieSignerCache: Application => CookieSigner = Application.instanceCache[CookieSigner]

  // Temporary placeholder until we can move out Session / Cookie singleton objects
  def cookieSigner: CookieSigner = {
    Play.privateMaybeApplication match {
      case Success(app) => cookieSignerCache(app)
      case Failure(cause) => throw new RuntimeException("The global cookie signer instance requires a running application.", cause)
    }
  }

}
