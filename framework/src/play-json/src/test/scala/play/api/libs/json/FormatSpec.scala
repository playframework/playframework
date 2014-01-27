/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.functional.syntax._

object FormatSpec extends Specification {
	case class User(id: Long, name: String)
	val luigi = User(1, "Luigi")

	"Format" should {

		import play.api.data.mapping._
		import play.api.data.mapping.json.Rules
		import play.api.data.mapping.json.Writes

		"serialize and deserialize primitives" in {
			import Rules._
			import Writes._

			val f = Formatting[JsValue, JsObject] { __ =>
				(__ \ "id").format[Long]
			}

			val m = Json.obj("id" -> 1L)

      f.writes(1L) mustEqual(m)
      f.validate(m) mustEqual(Success(1L))

      (Path \ "id").from[JsValue](f).validate(Json.obj()) mustEqual(Failure(Seq(Path \ "id" -> Seq(ValidationError("error.required")))))
    }


    "serialize and deserialize String" in {
			import Rules._
			import Writes._

			val f = Formatting[JsValue, JsObject] { __ =>
				(__ \ "id").format[String]
			}

			val m = Json.obj("id" -> "CAFEBABE")

      f.writes("CAFEBABE") mustEqual(m)
      f.validate(m) mustEqual(Success("CAFEBABE"))

      (Path \ "id").from[JsValue](f).validate(Json.obj()) mustEqual(Failure(Seq(Path \ "id" -> Seq(ValidationError("error.required")))))
    }

    "serialize and deserialize Seq[String]" in {
			import Rules._
			import Writes._

			val f = Formatting[JsValue, JsObject] { __ => (__ \ "ids").format[Seq[String]] }
			val m = Json.obj("ids" -> Seq("CAFEBABE", "FOOBAR"))

			f.validate(m) mustEqual(Success(Seq("CAFEBABE", "FOOBAR")))
      f.writes(Seq("CAFEBABE", "FOOBAR")) mustEqual(m)
    }

    "serialize and deserialize User case class" in {
    	import Rules._
    	import Writes._

	    implicit val userF = Formatting[JsValue, JsObject] { __ =>
				((__ \ "id").format[Long] ~
			   (__ \ "name").format[String])(User.apply _, unlift(User.unapply _))
			}

			val m = Json.obj("id" -> 1L, "name" -> "Luigi")
			userF.validate(m) mustEqual(Success(luigi))
		}

		"support primitives types" in {
			import Rules._
    	import Writes._

      "Int" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Int] }.validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Int] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Int")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Int] }.validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Int")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n" \ "o").format[Int] }.validate(Json.obj("n" -> Json.obj("o" -> 4))) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n" \ "o").format[Int] }.validate(Json.obj("n" -> Json.obj("o" -> "foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("error.number", "Int")))))

        Formatting[JsValue, JsObject] { __ => (__ \ "n" \ "o" \ "p").format[Int] }.validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> 4)))) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n" \ "o" \ "p").format[Int] }.validate(Json.obj("n" -> Json.obj("o" -> Json.obj("p" -> "foo")))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("error.number", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("error.required"))))
        Formatting[JsValue, JsObject] { __ => (__ \ "foo").format[Int] }.validate(Json.obj("n" -> 4)) mustEqual(error)
      }

      "Short" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Short] }.validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Short] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Short")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Short] }.validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Short")))))
      }

      "Long" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Long] }.validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Long] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Long")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Long] }.validate(Json.obj("n" -> 4.8)) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Long")))))
      }

      "Float" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Float] }.validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Float] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Float")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Float] }.validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8F))
      }

      "Double" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Double] }.validate(Json.obj("n" -> 4)) mustEqual(Success(4))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Double] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Double")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Double] }.validate(Json.obj("n" -> 4.8)) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[jBigDecimal] }.validate(Json.obj("n" -> 4)) mustEqual(Success(new jBigDecimal("4")))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[jBigDecimal] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "BigDecimal")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[jBigDecimal] }.validate(Json.obj("n" -> 4.8)) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[BigDecimal] }.validate(Json.obj("n" -> 4)) mustEqual(Success(BigDecimal(4)))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[BigDecimal] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "BigDecimal")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[BigDecimal] }.validate(Json.obj("n" -> 4.8)) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        Formatting[JsValue, JsObject] { __ =>
          (__ \ "n").format(Rules.date, Writes.date)
        }.validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(f.parse("1985-09-10")))

        Formatting[JsValue, JsObject] { __ =>
          (__ \ "n").format(Rules.date, Writes.date)
        }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date", "yyyy-MM-dd")))))
      }

      "iso date" in {
        skipped("Can't test on CI")
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        Formatting[JsValue, JsObject] { __ =>
          (__ \ "n").format(Rules.isoDate, Writes.isoDate)
        }.validate(Json.obj("n" -> "1985-09-10T00:00:00+02:00")) mustEqual(Success(f.parse("1985-09-10")))

        Formatting[JsValue, JsObject] { __ =>
          (__ \ "n").format(Rules.isoDate, Writes.isoDate)
        }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date.isoformat")))))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaDate)
          }.validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(jd))

          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaDate)
          }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "time" in {
          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaTime, Writes.jodaTime)
          }.validate(Json.obj("n" -> dd.getTime)) mustEqual(Success(jd))

          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaTime)
          }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()

          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaLocalDate, Writes.jodaLocalDate)
          }.validate(Json.obj("n" -> ld.toString())) mustEqual(Success(ld))

          Formatting[JsValue, JsObject] { __ =>
            (__ \ "n").format(Rules.jodaLocalDate, Writes.jodaLocalDate)
          }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "")))))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())

        Formatting[JsValue, JsObject] { __ =>
          (__ \ "n").format(Rules.sqlDate, Writes.sqlDate)
        }.validate(Json.obj("n" -> "1985-09-10")) mustEqual(Success(ds))
      }

      "Boolean" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Boolean] }.validate(Json.obj("n" -> true)) mustEqual(Success(true))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Boolean] }.validate(Json.obj("n" -> "foo")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "String" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[String] }.validate(Json.obj("n" -> "foo")) mustEqual(Success("foo"))
        Formatting[JsValue, JsObject] { __ => (__ \ "o").format[String] }.validate(Json.obj("o.n" -> "foo")) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("error.required")))))
      }

      "Option" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Json.obj("n" -> true)) mustEqual(Success(Some(true)))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Json.obj()) mustEqual(Success(None))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Json.obj("foo" -> "bar")) mustEqual(Success(None))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Json.obj("n" -> "bar")) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "Map[String, Seq[V]]" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Map[String, Seq[String]]] }.validate(Json.obj("n" -> Json.obj("foo" -> Seq("bar")))) mustEqual(Success(Map("foo" -> Seq("bar"))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Map[String, Seq[Int]]] }.validate(Json.obj("n" -> Json.obj("foo" -> Seq(4), "bar" -> Seq(5)))) mustEqual(Success(Map("foo" -> Seq(4), "bar" -> Seq(5))))
        Formatting[JsValue, JsObject] { __ => (__ \ "x").format[Map[String, Int]] }.validate(Json.obj("n" -> Json.obj("foo" -> 4, "bar" -> "frack"))) mustEqual(Failure(Seq(Path \ "x" -> Seq(ValidationError("error.required")))))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Map[String, Seq[Int]]] }.validate(Json.obj("n" -> Json.obj("foo" -> Seq(4), "bar" -> Seq("frack")))) mustEqual(Failure(Seq(Path \ "n" \ "bar" \ 0 -> Seq(ValidationError("error.number", "Int")))))
      }

      "Traversable" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Traversable[String]] }.validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Traversable[Int]] }.validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Traversable[Int]] }.validate(Json.obj("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(
          Path \ "n" \ 0 -> Seq(ValidationError("error.number", "Int")),
          Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int"))
        )))
      }

      "Array" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Array[String]] }.validate(Json.obj("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Array[Int]] }.validate(Json.obj("n" -> Seq(1, 2, 3))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Array[Int]] }.validate(Json.obj("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(
          Path \ "n" \ 0 -> Seq(ValidationError("error.number", "Int")),
          Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int"))
        )))
      }

      "Seq" in {
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Seq[String]] }.validate(Json.obj("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Seq[Int]] }.validate(Json.obj("n" -> Seq(1, 2, 3))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[JsValue, JsObject] { __ => (__ \ "n").format[Seq[Int]] }.validate(Json.obj("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(
          Path \ "n" \ 0 -> Seq(ValidationError("error.number", "Int")),
          Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int"))
        )))
      }
    }

		"serialize and deserialize with validation" in {
			import Rules._
			import Writes._

			val f = Formatting[JsValue, JsObject] { __ =>
        ((__ \ "firstname").format(notEmpty) ~
         (__ \ "lastname").format(notEmpty)).tupled
      }

			val valid = Json.obj(
				"firstname" -> "Julien",
				"lastname" -> "Tournay")

			val invalid = Json.obj("lastname" -> "Tournay")

			val result = ("Julien", "Tournay")

      f.writes(result) mustEqual(valid)
      f.validate(valid) mustEqual(Success(result))

      f.validate(invalid) mustEqual(Failure(Seq((Path \ "firstname", Seq(ValidationError("error.required"))))))
    }

    "format seq" in {
    	import Rules._
    	import Writes._

    	val valid = Json.obj(
      "firstname" -> Seq("Julien"),
      "foobar" -> JsArray(),
      "lastname" -> "Tournay",
      "age" -> 27,
      "information" -> Json.obj(
      	"label" -> "Personal",
      	"email" -> "fakecontact@gmail.com",
      	"phones" -> Seq("01.23.45.67.89", "98.76.54.32.10")))

      def isNotEmpty[T <: Traversable[_]] = validateWith[T]("error.notEmpty"){ !_.isEmpty }

      Formatting[JsValue, JsObject] { __ => (__ \ "firstname").format[Seq[String]] }.validate(valid) mustEqual(Success(Seq("Julien")))
      Formatting[JsValue, JsObject] { __ => (__ \ "foobar").format[Seq[String]] }.validate(valid) mustEqual(Success(Seq()))
      Formatting[JsValue, JsObject] { __ => (__ \ "foobar").format(isNotEmpty[Seq[Int]]) }.validate(valid) mustEqual(Failure(Seq(Path \ "foobar" -> Seq(ValidationError("error.notEmpty")))))
    }

    "format recursive" in {
      case class RecUser(name: String, friends: Seq[RecUser] = Nil)
      val u = RecUser(
        "bob",
        Seq(RecUser("tom")))

      val m = Json.obj(
        "name" -> "bob",
        "friends" -> Seq(Json.obj("name" -> "tom", "friends" -> Json.arr())))

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))
      val m1 = Json.obj(
        "name" -> "bob",
        "friend" -> Json.obj("name" -> "tom"))

      "using explicit notation" in {
      	import Rules._
    		import Writes._

        lazy val w: Format[JsValue, JsObject, RecUser] = Formatting[JsValue, JsObject]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friends").format(seqR(w), seqW(w)))(RecUser.apply _, unlift(RecUser.unapply _))
        }
        w.validate(m) mustEqual Success(u)
        w.writes(u) mustEqual m

        lazy val w3: Format[JsValue, JsObject, User1] = Formatting[JsValue, JsObject]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friend").format(optionR(w3), optionW(w3)))(User1.apply _, unlift(User1.unapply _))
        }
        w3.validate(m1) mustEqual Success(u1)
        w3.writes(u1) mustEqual m1
      }

      "using implicit notation" in {
      	import Rules._
				import Writes._

        implicit lazy val w: Format[JsValue, JsObject, RecUser] = Formatting[JsValue, JsObject]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friends").format[Seq[RecUser]])(RecUser.apply _, unlift(RecUser.unapply _))
        }
        w.validate(m) mustEqual Success(u)
        w.writes(u) mustEqual m

        implicit lazy val w3: Format[JsValue, JsObject, User1] = Formatting[JsValue, JsObject]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friend").format[Option[User1]])(User1.apply _, unlift(User1.unapply _))
        }
        w3.validate(m1) mustEqual Success(u1)
        w3.writes(u1) mustEqual m1
      }
    }

		"work with Rule ans Write seamlessly" in {
			import Rules._
    	import Writes._

	    implicit val userF = Formatting[JsValue, JsObject] { __ =>
				((__ \ "id").format[Long] ~
			   (__ \ "name").format[String])(User.apply _, unlift(User.unapply _))
			}

			val  userJs = Json.obj("id" -> 1L, "name" -> "Luigi")
			userF.validate(userJs) mustEqual(Success(luigi))
			userF.writes(luigi) mustEqual(userJs)

			val fin = From[JsObject] { __ =>
				(__ \ "user").read[User]
			}

			val m2 = Json.obj("user" -> userJs)
			fin.validate(m2) mustEqual(Success(luigi))

			val win = To[JsValue] { __ =>
				(__ \ "user").write[User]
			}
			win.writes(luigi) mustEqual(m2)
		}

	}

}