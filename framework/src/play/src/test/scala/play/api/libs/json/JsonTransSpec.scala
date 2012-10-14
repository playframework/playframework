package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.util.control.Exception._
import java.text.ParseException
import play.api.data.validation.ValidationError
import Reads.constraints._
import play.api.libs.json.util._


object JsonTransSpec extends Specification {
  "JSON transformers " should {
    val js = Json.obj(
      "field1" -> "alpha",
      "field2" -> 123L,
      "field3" -> Json.obj(
        "field31" -> "beta", 
        "field32"-> 345
      ),
      "field4" -> Json.arr("alpha", 2, true, Json.obj("field41" -> "toto", "field42" -> "tata"))
    )

    "pick a value at a path" in {
      js.validate(
        (__ \ 'field3).json.pick
      ).get must beEqualTo(
        Json.obj(
          "field31" -> "beta", "field32"-> 345
        )
      )
    }

    "pick a branch" in {
      js.validate(
        (__ \ 'field3).json.pickBranch
      ).get must beEqualTo(
        Json.obj(
          "field3" -> Json.obj("field31" -> "beta", "field32"-> 345)
        )
      )
    }

    "copy input JSON and update a branch (merge the updated branch with input JSON)" in {
      js.validate(
        (__ \ 'field3).json.update( 
          __.read[JsObject].map{ o => o ++ Json.obj( "field33" -> false ) }
        )
      ).get must beEqualTo(
        Json.obj(
          "field1" -> "alpha",
          "field2" -> 123L,
          "field3" -> Json.obj(
            "field31" -> "beta", 
            "field32" -> 345,
            "field33" -> false
          ),
          "field4" -> Json.arr("alpha", 2, true, Json.obj("field41" -> "toto", "field42" -> "tata"))
        )
      )
    }

    "pick a branch and update its content" in {
      js.validate(
        (__ \ 'field3).json.pickBranch(
          (__ \ 'field32).json.update( 
            of[JsNumber].map{ case JsNumber(nb) => JsNumber(nb + 12) }
          ) andThen 
          (__ \ 'field31).json.update( 
            of[JsString].map{ case JsString(s) => JsString(s + "toto") }
          )
        )
      ).get must beEqualTo(
        Json.obj(
          "field3" -> Json.obj("field31" -> "betatoto", "field32"-> 357)
        )
      )
    }

    "put a value in a new branch (don't keep passed json)" in {
      js.validate(
        (__ \ 'field3).json.put(JsNumber(234))
      ).get must beEqualTo(
        Json.obj(
          "field3" -> 234
        )
      )
    }

    "create a new path by copying a branch" in {
      js.validate(
        (__ \ 'field5).json.copyFrom( (__ \ 'field3).json.pick )
      ).get must beEqualTo(
        Json.obj(
          "field5" -> Json.obj(
            "field31" -> "beta", 
            "field32" -> 345
          )
        )
      )
    }

    "copy full json and prune a branch" in {
      js.validate(
        (__ \ 'field3).json.prune
      ).get must beEqualTo(
        Json.obj(
          "field1" -> "alpha",
          "field2" -> 123L,
          "field4" -> Json.arr("alpha", 2, true, Json.obj("field41" -> "toto", "field42" -> "tata"))
        )
      )
    }

    "pick a single branch and prune a sub-branch" in {
      js.validate(
        (__ \ 'field3).json.pickBranch(
          (__ \ 'field32).json.prune
        )
      ).get must beEqualTo(
        Json.obj(
          "field3" -> Json.obj("field31" -> "beta")
        )
      )
    }

    "copy the full json and update a 2nd-level path and then prune a subbranch" in {
      js.validate(
        (__ \ 'field3 \ 'field32).json.update(
          of[JsNumber].map{ case JsNumber(nb) => JsNumber(nb + 5) }
        ) andThen (__ \ 'field4).json.prune
      ).get must beEqualTo(
        Json.obj(
          "field1" -> "alpha",
          "field2" -> 123L,
          "field3" -> Json.obj(
            "field31" -> "beta", 
            "field32"-> 350
          )
        )
      )
    }
  }
}
