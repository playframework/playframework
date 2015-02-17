/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._

import scala.util.control.Exception._
import java.text.ParseException

object JsonRichSpec extends Specification {

  "JSON" should {
    "create json with rich syntax" in {
      val js = Json.obj(
        "key1" -> Json.obj("key11" -> "value11", "key12" -> 123L, "key13" -> JsNull),
        "key2" -> 123,
        "key3" -> true,
        "key4" -> Json.arr("value41", 345.6, JsString("test"), JsObject(Seq("key411" -> obj("key4111" -> 987.654))))
      )

      js must equalTo(
        JsObject(Seq(
          "key1" -> JsObject(Seq(
            "key11" -> JsString("value11"),
            "key12" -> JsNumber(123L),
            "key13" -> JsNull
          )),
          "key2" -> JsNumber(123),
          "key3" -> JsBoolean(true),
          "key4" -> JsArray(Seq(
            JsString("value41"), JsNumber(345.6),
            JsString("test"), JsObject(Seq("key411" -> JsObject(Seq("key4111" -> JsNumber(987.654)))))
          ))
        ))
      )

    }
  }

}

