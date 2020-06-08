/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import org.specs2.mutable.Specification
import play.api.libs.typedmap.TypedMap
import play.api.mvc.RequestHeaderImpl
import play.api.routing.HandlerDef

class ServerSpec extends Specification {

  private def handlerDef(modifiers: Seq[String]) = HandlerDef(null, null, null, null, null, null, null, null, modifiers)

  private def req(modifiers: String*) =
    new RequestHeaderImpl(null, null, null, null, null, TypedMap.empty)
      .addAttr(play.api.routing.Router.Attrs.HandlerDef, handlerDef(modifiers))

  "body parsing should be" should {
    "not deferred when deactivated globally and no route modifier exists" in {
      Server.routeModifierDefersBodyParsing(false, req()) must beFalse
    }
    "deferred when activated globally and no route modifier exists" in {
      Server.routeModifierDefersBodyParsing(true, req()) must beTrue
    }
    "deferred when deactivated globally but activated via route modifier" in {
      Server.routeModifierDefersBodyParsing(false, req("deferBodyParsing")) must beTrue
    }
    "deferred when deactivated globally but activated via case insensitive route modifier" in {
      Server.routeModifierDefersBodyParsing(false, req("dEfErBOdyPaRSING")) must beTrue
    }
    "deferred when both activated globally and also activated via route modifier" in {
      Server.routeModifierDefersBodyParsing(true, req("deferBodyParsing")) must beTrue
    }
    "not deferred when activated globally but deactivated via route modifier" in {
      Server.routeModifierDefersBodyParsing(true, req("dontDeferBodyParsing")) must beFalse
    }
    "not deferred when activated globally but deactivated via case insensitive route modifier" in {
      Server.routeModifierDefersBodyParsing(true, req("doNTDeFErBoDyPARSING")) must beFalse
    }
    "not deferred when deactivated globally and also deactivated via route modifier" in {
      Server.routeModifierDefersBodyParsing(false, req("dontDeferBodyParsing")) must beFalse
    }
    "not deferred when activated globally and also via route modifier but deactivated via route modifier" in {
      Server.routeModifierDefersBodyParsing(true, req("deferBodyParsing", "dontDeferBodyParsing")) must beFalse
    }
    "not deferred when deactivated globally and activated via route modifier but deactivated via route modifier" in {
      Server.routeModifierDefersBodyParsing(false, req("deferBodyParsing", "dontDeferBodyParsing")) must beFalse
    }
  }

}
