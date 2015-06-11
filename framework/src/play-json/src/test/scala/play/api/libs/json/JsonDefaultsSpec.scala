package play.api.libs.json

import org.specs2.mutable.Specification

case class SimpleDefaultArgs(intValue: Int = 5, stringValue: String = "foobar")

case class MixedDefaultsAndNonDefaults(
  intNoDefault: Int,
  intWithDefault: Int = 5,
  stringNoDefault: String,
  stringWithDefault: String = "foobar")

case class ComplexDefaultArgs(
  listOfInt: List[Int] = List(1, 2, 3),
  optOfDouble: Option[Double] = Some(123.45))

case class Inner(x: Int)
case class NestedDefaultArgs(inner: Inner = Inner(123))

object JsonDefaultsSpec extends Specification {

  "Json.reads" should {
    "support simple default values" in {
      implicit val reads = Json.reads[SimpleDefaultArgs]

      Json.fromJson[SimpleDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(SimpleDefaultArgs()))
    }

    "support mixed params with default and non-default values" in {
      implicit val reads = Json.reads[MixedDefaultsAndNonDefaults]

      val json = Json.obj("intNoDefault" -> 6, "stringNoDefault" -> "abc")
      val expected = JsSuccess(MixedDefaultsAndNonDefaults(intNoDefault = 6, stringNoDefault = "abc"))
      Json.fromJson[MixedDefaultsAndNonDefaults](json) must beEqualTo(expected)
    }

    "support complex default values" in {
      implicit val reads = Json.reads[ComplexDefaultArgs]

      Json.fromJson[ComplexDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(ComplexDefaultArgs()))
    }

    "support nested case classes" in {
      implicit val readsInner = Json.reads[Inner]
      implicit val readsOuter = Json.reads[NestedDefaultArgs]

      Json.fromJson[NestedDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(NestedDefaultArgs()))
    }

    "fail on mismatched types" in {
      implicit val reads = Json.reads[SimpleDefaultArgs]

      val expected = JsError(__ \ "intValue", "error.expected.jsnumber")
      Json.fromJson[SimpleDefaultArgs](Json.obj("intValue" -> "not an int")) must beEqualTo(expected)

      val expected2 = JsError(__ \ "stringValue", "error.expected.jsstring")
      Json.fromJson[SimpleDefaultArgs](Json.obj("stringValue" -> 42)) must beEqualTo(expected2)
    }
  }

  "Json.writes" should {
    "support simple default values" in {
      implicit val writes = Json.writes[SimpleDefaultArgs]

      val expected = Json.obj("intValue" -> 5, "stringValue" -> "foobar")
      Json.toJson(SimpleDefaultArgs()) must beEqualTo(expected)
    }

    "support mixed params with default and non-default values" in {
      implicit val writes = Json.writes[MixedDefaultsAndNonDefaults]

      val obj = MixedDefaultsAndNonDefaults(intNoDefault = 6, stringNoDefault = "abc")
      val expected = Json.obj("intNoDefault" -> 6, "intWithDefault" -> 5, "stringNoDefault" -> "abc", "stringWithDefault" -> "foobar")
      Json.toJson(obj) must beEqualTo(expected)
    }

    "support complex default values" in {
      implicit val writes = Json.writes[ComplexDefaultArgs]

      val expected = Json.obj("listOfInt" -> List(1, 2, 3), "optOfDouble" -> 123.45)
      Json.toJson(ComplexDefaultArgs()) must beEqualTo(expected)
    }

    "support nested case classes" in {
      implicit val writesInner = Json.writes[Inner]
      implicit val writesOuter = Json.writes[NestedDefaultArgs]

      val expected = Json.obj("inner" -> Json.obj("x" -> 123))
      Json.toJson(NestedDefaultArgs()) must beEqualTo(expected)
    }
  }

  "Json.format" should {
    "support simple default values" in {
      implicit val format = Json.format[SimpleDefaultArgs]

      Json.fromJson[SimpleDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(SimpleDefaultArgs()))
      Json.toJson[SimpleDefaultArgs](SimpleDefaultArgs()) must beEqualTo(Json.obj("intValue" -> 5, "stringValue" -> "foobar"))
    }

    "support mixed params with default and non-default values" in {
      implicit val format = Json.format[MixedDefaultsAndNonDefaults]

      val obj = MixedDefaultsAndNonDefaults(intNoDefault = 6, stringNoDefault = "abc")
      val json = Json.obj("intNoDefault" -> 6, "stringNoDefault" -> "abc")
      Json.fromJson[MixedDefaultsAndNonDefaults](json) must beEqualTo(JsSuccess(obj))

      val expected = json ++ Json.obj("intWithDefault" -> 5, "stringWithDefault" -> "foobar")
      Json.toJson(obj) must beEqualTo(expected)
    }

    "support complex default values" in {
      implicit val format = Json.format[ComplexDefaultArgs]

      Json.fromJson[ComplexDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(ComplexDefaultArgs()))

      val expected = Json.obj("listOfInt" -> List(1, 2, 3), "optOfDouble" -> 123.45)
      Json.toJson(ComplexDefaultArgs()) must beEqualTo(expected)
    }

    "support nested case classes" in {
      implicit val formatInner = Json.format[Inner]
      implicit val formatOuter = Json.format[NestedDefaultArgs]

      Json.fromJson[NestedDefaultArgs](Json.obj()) must beEqualTo(JsSuccess(NestedDefaultArgs()))

      val expected = Json.obj("inner" -> Json.obj("x" -> 123))
      Json.toJson(NestedDefaultArgs()) must beEqualTo(expected)
    }
  }
}
