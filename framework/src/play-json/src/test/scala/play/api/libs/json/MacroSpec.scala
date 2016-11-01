/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

object TestFormats {
  implicit def eitherReads[A: Reads, B: Reads] = Reads[Either[A, B]] { js =>
    implicitly[Reads[A]].reads(js) match {
      case JsSuccess(a, _) => JsSuccess(Left(a))
      case _ => implicitly[Reads[B]].reads(js).map(Right(_))
    }
  }

  implicit def eitherWrites[A: Writes, B: Writes] = Writes[Either[A, B]] {
    case Left(a) => implicitly[Writes[A]].writes(a)
    case Right(b) => implicitly[Writes[B]].writes(b)
  }

  implicit def tuple2Reads[A: Reads, B: Reads]: Reads[(A, B)] = Reads { js =>
    for {
      a <- (js \ "_1").validate[A]
      b <- (js \ "_2").validate[B]
    } yield a -> b
  }

  implicit def tuple2OWrites[A: Writes, B: Writes]: OWrites[(A, B)] =
    OWrites {
      case (a, b) => Json.obj("_1" -> a, "_2" -> b)
    }
}

class MacroSpec extends org.specs2.mutable.Specification {
  "JSON macros" title

  "Reads" should {
    "be generated for simple case class" in {
      Json.reads[Simple].reads(Json.obj("bar" -> "lorem")).
        get must_== Simple("lorem")
    }

    "as Format for a simple generic case class" in {
      val fmt = Json.format[Lorem[Double]]

      fmt.reads(Json.obj("ipsum" -> 0.123D, "age" -> 1)).get must_== Lorem(
        0.123D, 1)
    }

    "refuse value other than JsObject when properties are optional" in {
      val r = Json.reads[Optional]
      val f = Json.format[Optional]

      r.reads(Json.obj()).get must_== Optional(None) and {
        r.reads(JsString("foo")).asEither must beLeft.like {
          case (_, Seq(err)) :: Nil =>
            err.message must_== "error.expected.jsobject"
        }
      } and {
        f.reads(Json.obj()).get must_== Optional(None)
      } and {
        f.reads(JsString("foo")).asEither must beLeft.like {
          case (_, Seq(err)) :: Nil =>
            err.message must_== "error.expected.jsobject"
        }
      }
    }
  }

  "Writes" should {
    "be generated for simple case class" in {
      Json.writes[Simple].writes(Simple("lorem")) must_== Json.obj("bar" -> "lorem")
    }

    "as Format for a generic case class" in {
      val fmt = Json.format[Lorem[Float]]

      fmt.writes(Lorem(2.34F, 2)) must_== Json.obj(
        "ipsum" -> 2.34F, "age" -> 2)
    }
  }

  "Macro" should {
    "handle case class with self type as nested type parameter" >> {
      import TestFormats._

      val jsonNoValue = Json.obj("id" -> 1L)
      val jsonStrValue = Json.obj("id" -> 2L, "value" -> "str")
      val jsonFooValue = Json.obj("id" -> 3L, "value" -> jsonStrValue)

      val fooStrValue = Foo(2L, Some(Left("str")))
      val fooFooValue = Foo(3L, Some(Right(fooStrValue)))

      def readSpec(r: Reads[Foo]) = {
        r.reads(jsonNoValue).get must_== Foo(1L, None) and {
          r.reads(jsonStrValue).get must_== fooStrValue
        } and {
          r.reads(jsonFooValue).get must_== fooFooValue
        } and {
          r.reads(Json.obj("id" -> 4L, "value" -> jsonFooValue)).
            get must_== Foo(4L, Some(Right(fooFooValue)))
        }
      }

      def writeSpec(w: Writes[Foo]) = {
        w.writes(Foo(1L, None)) must_== jsonNoValue and {
          w.writes(fooStrValue) must_== jsonStrValue
        } and {
          w.writes(fooFooValue) must_== jsonFooValue
        } and {
          w.writes(Foo(4L, Some(Right(fooFooValue)))) must_== Json.
            obj("id" -> 4L, "value" -> jsonFooValue)
        }
      }

      "to generate Reads" in readSpec(Json.reads[Foo])

      "to generate Writes" in writeSpec(Json.writes[Foo])

      "to generate Format" in {
        val f: OFormat[Foo] = Json.format[Foo]

        readSpec(f) and writeSpec(f)
      }
    }

    "handle generic case class with multiple generic parameters" >> {
      val jsonNoOther = Json.obj("base" -> 1)
      val jsonOther = Json.obj("base" -> 2, "other" -> 3)

      val noOther = Interval(1, None)
      val other = Interval(2, Some(3))

      def readSpec(r: Reads[Interval[Int]]) =
        r.reads(jsonNoOther).get must_== noOther and {
          r.reads(jsonOther).get must_== other
        }

      def writeSpec(r: Writes[Interval[Int]]) =
        r.writes(noOther) must_== jsonNoOther and {
          r.writes(other) must_== jsonOther
        }

      "to generate Reads" in readSpec(Json.reads[Interval[Int]])

      "to generate Writes" in writeSpec(Json.writes[Interval[Int]])

      "to generate Format" in {
        val f = Json.format[Interval[Int]]
        readSpec(f) and writeSpec(f)
      }
    }

    "handle case class with implicits" >> {
      val json1 = Json.obj("pos" -> 2, "text" -> "str")
      val json2 = Json.obj("ident" -> "id", "value" -> 23.456D)
      val fixture1 = WithImplicit1(2, "str")
      val fixture2 = WithImplicit2("id", 23.456D)

      def readSpec1(r: Reads[WithImplicit1]) =
        r.reads(json1).get must_== fixture1

      def writeSpec2(w: OWrites[WithImplicit2[Double]]) =
        w.writes(fixture2) must_== json2

      "to generate Reads" in readSpec1(Json.reads[WithImplicit1])

      "to generate Writes with type parameters" in writeSpec2(
        Json.writes[WithImplicit2[Double]])

      "to generate Format" in {
        val f1 = Json.format[WithImplicit1]
        val f2 = Json.format[WithImplicit2[Double]]

        readSpec1(f1) and {
          f1.writes(fixture1) must_== json1
        } and writeSpec2(f2) and {
          f2.reads(json2).get must_== fixture2
        }
      }
    }

    "handle generic case class with multiple generic parameters and self references" >> {
      import TestFormats._

      val nestedLeft = Json.obj("id" -> 2, "a" -> 0.2F, "b" -> 0.3F, "c" -> 3)

      val nestedRight = Json.obj(
        "id" -> 1, "a" -> 0.1F, "b" -> "right1", "c" -> 2)

      val jsonRight = Json.obj(
        "id" -> 3, "a" -> nestedRight, "b" -> "right2", "c" -> 0.4D)

      val jsonLeft = Json.obj(
        "id" -> 4, "a" -> nestedLeft, "b" -> nestedRight, "c" -> 0.5D)

      val complexRight = Complex(3, Complex(1, 0.1F, Right("right1"), 2),
        Right("right2"), 0.4D)

      val complexLeft = Complex(4, Complex(2, 0.2F, Left(0.3F), 3),
        Left(Complex(1, 0.1F, Right("right1"), 2)), 0.5D)

      def readSpec(r: Reads[Complex[Complex[Float, Int], Double]]) = {
        r.reads(jsonRight).get must_== complexRight and {
          r.reads(jsonLeft).get must_== complexLeft
        }
      }

      def writeSpec(r: Writes[Complex[Complex[Float, Int], Double]]) = {
        r.writes(complexRight) must_== jsonRight and {
          r.writes(complexLeft) must_== jsonLeft
        }
      }

      "to generate Reads" in readSpec {
        implicit val nested = Json.reads[Complex[Float, Int]]
        Json.reads[Complex[Complex[Float, Int], Double]]
      }

      "to generate Writes" in writeSpec {
        implicit val nested = Json.writes[Complex[Float, Int]]
        Json.writes[Complex[Complex[Float, Int], Double]]
      }

      "to generate Writes" in {
        implicit val nested = Json.format[Complex[Float, Int]]
        val f = Json.format[Complex[Complex[Float, Int], Double]]

        readSpec(f) and writeSpec(f)
      }
    }

    "handle case class with collection types" >> {
      import TestFormats._

      val json = Json.obj(
        "id" -> "foo",
        "ls" -> List(1.2D, 23.45D),
        "set" -> List(1, 3, 4, 7),
        "seq" -> List(
          Json.obj("_1" -> 2, "_2" -> "bar"),
          Json.obj("_1" -> 4, "_2" -> "lorem"),
          Json.obj("_2" -> "ipsum", "_1" -> 5)
        ),
        "scores" -> Json.obj("A1" -> 0.1F, "EF" -> 12.3F)
      )
      val withColl = WithColl(
        id = "foo",
        ls = List(1.2D, 23.45D),
        set = Set(1, 3, 4, 7),
        seq = Seq(2 -> "bar", 4 -> "lorem", 5 -> "ipsum"),
        scores = Map("A1" -> 0.1F, "EF" -> 12.3F)
      )

      def readSpec(r: Reads[WithColl[Double, (Int, String)]]) =
        r.reads(json).get must_== withColl

      def writeSpec(w: Writes[WithColl[Double, (Int, String)]]) =
        w.writes(withColl) must_== json

      "to generated Reads" in readSpec {
        Json.reads[WithColl[Double, (Int, String)]]
      }

      "to generated Writes" in writeSpec {
        Json.writes[WithColl[Double, (Int, String)]]
      }

      "to generate Format" in {
        val f = Json.format[WithColl[Double, (Int, String)]]
        readSpec(f) and writeSpec(f)
      }
    }
  }

  // ---

  case class Simple(bar: String)
  case class Lorem[T](ipsum: T, age: Int)
  case class Optional(prop: Option[String])

  case class Foo(id: Long, value: Option[Either[String, Foo]])
  case class Interval[T](base: T, other: Option[T])
  case class Complex[T, U](id: Int, a: T, b: Either[T, String], c: U)

  case class WithImplicit1(pos: Int, text: String)(implicit x: Numeric[Int])
  case class WithImplicit2[N: Numeric](ident: String, value: N)

  case class WithColl[A: Numeric, B](
    id: String,
    ls: List[A],
    set: Set[Int],
    seq: Seq[B],
    scores: Map[String, Float])
}
