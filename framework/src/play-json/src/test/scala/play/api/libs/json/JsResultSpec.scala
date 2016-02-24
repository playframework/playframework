/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import play.api.libs.functional.Functor

import JsResult.functorJsResult

object JsResultSpec extends org.specs2.mutable.Specification {

  title("JSON result")

  "Result" should {
    "be functor" in {
      val jsres = JsSuccess("jsStr")

      implicitly[Functor[JsResult]].fmap[String, List[Char]](jsres, _.toList).
        aka("JSON result") must_== JsSuccess(List('j', 's', 'S', 't', 'r'))
    }
  }
}
