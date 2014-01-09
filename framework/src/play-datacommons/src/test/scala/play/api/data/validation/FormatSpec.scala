/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data.mapping

import org.specs2.mutable._
import play.api.libs.functional.syntax._

object FormatSpec extends Specification {
	case class User(id: Long, name: String)
	val luigi = User(1, "Luigi")

	"Format" should {

		"serialize and deserialize primitives" in {
			import Rules._
			import Writes._

			val f = Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
				(__ \ "id").format[Long]
			}

			val m = Map("id" -> Seq("1"))

      f.writes(1L) mustEqual(m)
      f.validate(m) mustEqual(Success(1L))

      (Path \ "id").from[UrlFormEncoded](f).validate(Map.empty) mustEqual(Failure(Seq(Path \ "id" -> Seq(ValidationError("error.required")))))
    }


    "serialize and deserialize String" in {
			import Rules._
			import Writes._

			val f = Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
				(__ \ "id").format[String]
			}

			val m = Map("id" -> Seq("CAFEBABE"))

      f.writes("CAFEBABE") mustEqual(m)
      f.validate(m) mustEqual(Success("CAFEBABE"))

      (Path \ "id").from[UrlFormEncoded](f).validate(Map.empty) mustEqual(Failure(Seq(Path \ "id" -> Seq(ValidationError("error.required")))))
    }

    "serialize and deserialize Seq[String]" in {
			import Rules._
			import Writes._

			val f = Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "ids").format[Seq[String]] }
			val m = Map("ids[0]" -> Seq("CAFEBABE"), "ids[1]" -> Seq("FOOBAR"))

			f.validate(m) mustEqual(Success(Seq("CAFEBABE", "FOOBAR")))
      f.writes(Seq("CAFEBABE", "FOOBAR")) mustEqual(m)
    }

    "serialize and deserialize User case class" in {
    	import Rules._
    	import Writes._

	    implicit val userF: Format[UrlFormEncoded, UrlFormEncoded, User] = Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
				((__ \ "id").format[Long] ~
			   (__ \ "name").format[String])(User.apply _, unlift(User.unapply _))
			}

			val m = Map("id" -> Seq("1"), "name" -> Seq("Luigi"))
			userF.validate(m) mustEqual(Success(luigi))

			// val fin = From[UrlFormEncoded] { __ =>
			// 	(__ \ "user").read[User]
			// }

			// val m2 = Map("user.id" -> Seq("1"), "user.name" -> Seq("Luigi"))
			// fin.validate(m2) mustEqual(Success(luigi))
		}

		"support primitives types" in {
			import Rules._
    	import Writes._

      "Int" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Int] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Int] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Int")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Int] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Int")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n" \ "o").format[Int] }.validate(Map("n.o" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n" \ "o").format[Int] }.validate(Map("n.o" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" -> Seq(ValidationError("error.number", "Int")))))

        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").format[Int] }.validate(Map("n.o.p" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n" \ "o" \ "p").format[Int] }.validate(Map("n.o.p" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" \ "o" \ "p" -> Seq(ValidationError("error.number", "Int")))))

        val errPath = Path \ "foo"
        val error = Failure(Seq(errPath -> Seq(ValidationError("error.required"))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "foo").format[Int] }.validate(Map("n" -> Seq("4"))) mustEqual(error)
      }

      "Short" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Short] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Short] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Short")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Short] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Short")))))
      }

      "Long" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Long] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Long] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Long")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Long] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Long")))))
      }

      "Float" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Float] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Float] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Float")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Float] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8F))
      }

      "Double" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Double] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(4))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Double] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "Double")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Double] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(4.8))
      }

      "java BigDecimal" in {
        import java.math.{ BigDecimal => jBigDecimal }
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[jBigDecimal] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(new jBigDecimal("4")))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[jBigDecimal] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "BigDecimal")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[jBigDecimal] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(new jBigDecimal("4.8")))
      }

      "scala BigDecimal" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[BigDecimal] }.validate(Map("n" -> Seq("4"))) mustEqual(Success(BigDecimal(4)))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[BigDecimal] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.number", "BigDecimal")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[BigDecimal] }.validate(Map("n" -> Seq("4.8"))) mustEqual(Success(BigDecimal(4.8)))
      }

      "date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
          (__ \ "n").format(Rules.date, Writes.date)
        }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(f.parse("1985-09-10")))

        Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
          (__ \ "n").format(Rules.date, Writes.date)
        }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date", "yyyy-MM-dd")))))
      }

      "iso date" in {
        skipped("Can't test on CI")
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
          (__ \ "n").format(Rules.isoDate, Writes.isoDate)
        }.validate(Map("n" -> Seq("1985-09-10T00:00:00+02:00"))) mustEqual(Success(f.parse("1985-09-10")))

        Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
          (__ \ "n").format(Rules.isoDate, Writes.isoDate)
        }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.date.isoformat")))))
      }

      "joda" in {
        import org.joda.time.DateTime
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val jd = new DateTime(dd)

        "date" in {
          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaDate)
          }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(jd))

          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaDate)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "time" in {
          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaTime, Writes.jodaTime)
          }.validate(Map("n" -> Seq(dd.getTime.toString))) mustEqual(Success(jd))

          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaDate, Writes.jodaTime)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "local date" in {
          import org.joda.time.LocalDate
          val ld = new LocalDate()

          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaLocalDate, Writes.jodaLocalDate)
          }.validate(Map("n" -> Seq(ld.toString()))) mustEqual(Success(ld))

          Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
            (__ \ "n").format(Rules.jodaLocalDate, Writes.jodaLocalDate)
          }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.expected.jodadate.format", "")))))
        }
      }

      "sql date" in {
        import java.util.Date
        val f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val dd = f.parse("1985-09-10")
        val ds = new java.sql.Date(dd.getTime())

        Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
          (__ \ "n").format(Rules.sqlDate, Writes.sqlDate)
        }.validate(Map("n" -> Seq("1985-09-10"))) mustEqual(Success(ds))
      }

      "Boolean" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Boolean] }.validate(Map("n" -> Seq("true"))) mustEqual(Success(true))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Boolean] }.validate(Map("n" -> Seq("TRUE"))) mustEqual(Success(true))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Boolean] }.validate(Map("n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "String" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[String] }.validate(Map("n" -> Seq("foo"))) mustEqual(Success("foo"))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "o").format[String] }.validate(Map("o.n" -> Seq("foo"))) mustEqual(Failure(Seq(Path \ "o" -> Seq(ValidationError("error.required")))))
      }

      "Option" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Map("n" -> Seq("true"))) mustEqual(Success(Some(true)))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Map("n" -> Seq(""))) mustEqual(Success(None))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Map("foo" -> Seq("bar"))) mustEqual(Success(None))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Option[Boolean]] }.validate(Map("n" -> Seq("bar"))) mustEqual(Failure(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "Boolean")))))
      }

      "Map[String, Seq[V]]" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Map[String, Seq[String]]] }.validate(Map("n.foo" -> Seq("bar"))) mustEqual(Success(Map("foo" -> Seq("bar"))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Map[String, Seq[Int]]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("5"))) mustEqual(Success(Map("foo" -> Seq(4), "bar" -> Seq(5))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "x").format[Map[String, Int]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("frack"))) mustEqual(Failure(Seq(Path \ "x" -> Seq(ValidationError("error.required")))))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Map[String, Seq[Int]]] }.validate(Map("n.foo" -> Seq("4"), "n.bar" -> Seq("frack"))) mustEqual(Failure(Seq(Path \ "n" \ "bar" \ 0 -> Seq(ValidationError("error.number", "Int")))))
      }

      "Traversable" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Traversable[String]] }.validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Traversable[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Traversable[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int")))))
      }

      "Array" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Array[String]] }.validate(Map("n" -> Seq("foo"))).get.toSeq must haveTheSameElementsAs(Seq("foo"))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Array[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get.toSeq must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Array[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int")))))
      }

      "Seq" in {
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Seq[String]] }.validate(Map("n" -> Seq("foo"))).get must haveTheSameElementsAs(Seq("foo"))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Seq[Int]] }.validate(Map("n" -> Seq("1", "2", "3"))).get must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Seq[Int]] }.validate(Map(
          "n[0]" -> Seq("1"),
          "n[1]" -> Seq("2"),
          "n[3]" -> Seq("3")
        )).get must haveTheSameElementsAs(Seq(1, 2, 3))
        Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "n").format[Seq[Int]] }.validate(Map("n" -> Seq("1", "paf"))) mustEqual(Failure(Seq(Path \ "n" \ 1 -> Seq(ValidationError("error.number", "Int")))))
      }
    }

		"serialize and deserialize with validation" in {
			import Rules._
			import Writes._

			val f = Formatting[UrlFormEncoded, UrlFormEncoded] { __ =>
        ((__ \ "firstname").format(notEmpty) ~
         (__ \ "lastname").format(notEmpty)).tupled
      }

			val valid = Map(
				"firstname" -> Seq("Julien"),
				"lastname" -> Seq("Tournay"))

			val invalid = Map(
				"firstname" -> Seq(""),
				"lastname" -> Seq("Tournay"))

			val result = ("Julien", "Tournay")

      f.writes(result) mustEqual(valid)
      f.validate(valid) mustEqual(Success(result))

      f.validate(invalid) mustEqual(Failure(Seq((Path \ "firstname", Seq(ValidationError("error.required"))))))
    }

    "format seq" in {
    	import Rules._
    	import Writes._

    	val valid: UrlFormEncoded = Map(
      "firstname" -> Seq("Julien"),
      "lastname" -> Seq("Tournay"),
      "age" -> Seq("27"),
      "informations.label" -> Seq("Personal"),
      "informations.email" -> Seq("fakecontact@gmail.com"),
      "informations.phones" -> Seq("01.23.45.67.89", "98.76.54.32.10"))

      def isNotEmpty[T <: Traversable[_]] = validateWith[T]("error.notEmpty"){ !_.isEmpty }

      Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "firstname").format[Seq[String]] }.validate(valid) mustEqual(Success(Seq("Julien")))
      Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "foobar").format[Seq[String]] }.validate(valid) mustEqual(Success(Seq()))
      Formatting[UrlFormEncoded, UrlFormEncoded] { __ => (__ \ "foobar").format(isNotEmpty[Seq[Int]]) }.validate(valid) mustEqual(Failure(Seq(Path \ "foobar" -> Seq(ValidationError("error.notEmpty")))))
    }

    "format recursive" in {
      case class RecUser(name: String, friends: Seq[RecUser] = Nil)
      val u = RecUser(
        "bob",
        Seq(RecUser("tom")))

      val m = Map(
        "name" -> Seq("bob"),
        "friends[0].name" -> Seq("tom"),
        "friends[0].friends" -> Seq())

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))
      val m1 = Map(
        "name" -> Seq("bob"),
        "friend.name" -> Seq("tom"))

      "using explicit notation" in {
      	import Rules._
    		import Writes._

        lazy val w: Format[UrlFormEncoded, UrlFormEncoded, RecUser] = Formatting[UrlFormEncoded, UrlFormEncoded]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friends").format(seqR(w), seqW(w)))(RecUser.apply _, unlift(RecUser.unapply _))
        }
        w.validate(m) mustEqual Success(u)
        w.writes(u) mustEqual (m - "friends[0].friends")

        lazy val w3: Format[UrlFormEncoded, UrlFormEncoded, User1] = Formatting[UrlFormEncoded, UrlFormEncoded]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friend").format(optionR(w3), optionW(w3)))(User1.apply _, unlift(User1.unapply _))
        }
        w3.validate(m1) mustEqual Success(u1)
        w3.writes(u1) mustEqual m1
      }

      "using implicit notation" in {
      	import Rules._
				import Writes._

        implicit lazy val w: Format[UrlFormEncoded, UrlFormEncoded, RecUser] = Formatting[UrlFormEncoded, UrlFormEncoded]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friends").format[Seq[RecUser]])(RecUser.apply _, unlift(RecUser.unapply _))
        }
        w.validate(m) mustEqual Success(u)
        w.writes(u) mustEqual (m - "friends[0].friends")

        implicit lazy val w3: Format[UrlFormEncoded, UrlFormEncoded, User1] = Formatting[UrlFormEncoded, UrlFormEncoded]{ __ =>
          ((__ \ "name").format[String] ~
           (__ \ "friend").format[Option[User1]])(User1.apply _, unlift(User1.unapply _))
        }
        w3.validate(m1) mustEqual Success(u1)
        w3.writes(u1) mustEqual m1
      }
    }

	}

}