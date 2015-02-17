/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._
import play.api.libs.json.Json
import org.specs2.mutable.Specification
import org.joda.time.{ DateTime, LocalDate }

object FormSpec extends Specification {
  "A form" should {
    "have an error due to a malformed email" in {
      val f5 = ScalaForms.emailForm.fillAndValidate(("john@", "John"))
      f5.errors must haveSize(1)
      f5.errors.find(_.message == "error.email") must beSome

      val f6 = ScalaForms.emailForm.fillAndValidate(("john@zen.....com", "John"))
      f6.errors must haveSize(1)
      f6.errors.find(_.message == "error.email") must beSome
    }

    "be valid with a well-formed email" in {
      val f7 = ScalaForms.emailForm.fillAndValidate(("john@zen.com", "John"))
      f7.errors must beEmpty

      val f8 = ScalaForms.emailForm.fillAndValidate(("john@zen.museum", "John"))
      f8.errors must beEmpty

      val f9 = ScalaForms.emailForm.fillAndValidate(("john@mail.zen.com", "John"))
      f9.errors must beEmpty

      ScalaForms.emailForm.fillAndValidate(("o'flynn@example.com", "O'Flynn")).errors must beEmpty
    }

    "support mapping 22 fields" in {
      val form = Form(
        tuple(
          "k1" -> of[String],
          "k2" -> of[String],
          "k3" -> of[String],
          "k4" -> of[String],
          "k5" -> of[String],
          "k6" -> of[String],
          "k7" -> of[String],
          "k8" -> of[String],
          "k9" -> of[String],
          "k10" -> of[String],
          "k11" -> of[String],
          "k12" -> of[String],
          "k13" -> of[String],
          "k14" -> of[String],
          "k15" -> of[String],
          "k16" -> of[String],
          "k17" -> of[String],
          "k18" -> of[String],
          "k19" -> of[String],
          "k20" -> of[String],
          "k21" -> of[String],
          "k22" -> of[String]
        )
      )

      form.bind(Map(
        "k1" -> "v1",
        "k2" -> "v2",
        "k3" -> "v3",
        "k4" -> "v4",
        "k5" -> "v5",
        "k6" -> "v6",
        "k7" -> "v7",
        "k8" -> "v8",
        "k9" -> "v9",
        "k10" -> "v10",
        "k11" -> "v11",
        "k12" -> "v12",
        "k13" -> "v13",
        "k14" -> "v14",
        "k15" -> "v15",
        "k16" -> "v16",
        "k17" -> "v17",
        "k18" -> "v18",
        "k19" -> "v19",
        "k20" -> "v20",
        "k21" -> "v21",
        "k22" -> "v22"
      )).fold(_ => "errors", t => t._21) must_== "v21"
    }

    "apply constraints on wrapped mappings" in {
      "when it binds data" in {
        val f1 = ScalaForms.form.bind(Map("foo" -> "0"))
        f1.errors must haveSize(1)
        f1.errors.find(_.message == "first.digit") must beSome

        val f2 = ScalaForms.form.bind(Map("foo" -> "3"))
        f2.errors must beEmpty

        val f3 = ScalaForms.form.bind(Map("foo" -> "50"))
        f3.errors must haveSize(1) // Only one error because "number.42" can’t be applied since wrapped bind failed
        f3.errors.find(_.message == "first.digit") must beSome

        val f4 = ScalaForms.form.bind(Map("foo" -> "333"))
        f4.errors must haveSize(1)
        f4.errors.find(_.message == "number.42") must beSome
      }

      "when it is filled with data" in {
        val f1 = ScalaForms.form.fillAndValidate(0)
        f1.errors must haveSize(1)
        f1.errors.find(_.message == "first.digit") must beSome

        val f2 = ScalaForms.form.fillAndValidate(3)
        f2.errors must beEmpty

        val f3 = ScalaForms.form.fillAndValidate(50)
        f3.errors must haveSize(2)
        f3.errors.find(_.message == "first.digit") must beSome
        f3.errors.find(_.message == "number.42") must beSome

        val f4 = ScalaForms.form.fillAndValidate(333)
        f4.errors must haveSize(1)
        f4.errors.find(_.message == "number.42") must beSome
      }
    }

    "apply constraints on longNumber fields" in {
      val f1 = ScalaForms.longNumberForm.fillAndValidate(0)
      f1.errors must haveSize(1)
      f1.errors.find(_.message == "error.min") must beSome

      val f2 = ScalaForms.longNumberForm.fillAndValidate(9000)
      f2.errors must haveSize(1)
      f2.errors.find(_.message == "error.max") must beSome

      val f3 = ScalaForms.longNumberForm.fillAndValidate(10)
      f3.errors must beEmpty

      val f4 = ScalaForms.longNumberForm.fillAndValidate(42)
      f4.errors must beEmpty
    }

    "apply constraints on shortNumber fields" in {
      val f1 = ScalaForms.shortNumberForm.fillAndValidate(0)
      f1.errors must haveSize(1)
      f1.errors.find(_.message == "error.min") must beSome

      val f2 = ScalaForms.shortNumberForm.fillAndValidate(9000)
      f2.errors must haveSize(1)
      f2.errors.find(_.message == "error.max") must beSome

      val f3 = ScalaForms.shortNumberForm.fillAndValidate(10)
      f3.errors must beEmpty

      val f4 = ScalaForms.shortNumberForm.fillAndValidate(42)
      f4.errors must beEmpty
    }

    "apply constraints on byteNumber fields" in {
      val f1 = ScalaForms.byteNumberForm.fillAndValidate(0)
      f1.errors must haveSize(1)
      f1.errors.find(_.message == "error.min") must beSome

      val f2 = ScalaForms.byteNumberForm.fillAndValidate(9000)
      f2.errors must haveSize(1)
      f2.errors.find(_.message == "error.max") must beSome

      val f3 = ScalaForms.byteNumberForm.fillAndValidate(10)
      f3.errors must beEmpty

      val f4 = ScalaForms.byteNumberForm.fillAndValidate(42)
      f4.errors must beEmpty
    }

    "not even attempt to validate on fill" in {
      val failingValidatorForm = Form(
        "foo" -> Forms.text.verifying("isEmpty", s =>
          if (s.isEmpty) true
          else throw new AssertionError("Validation was run when it wasn't meant to")
        )
      )
      failingValidatorForm.fill("foo").errors must beEmpty
    }
  }

  "render form using field[Type] syntax" in {
    val anyData = Map("email" -> "bob@gmail.com", "password" -> "123")
    ScalaForms.loginForm.bind(anyData).get.toString must equalTo("(bob@gmail.com,123)")
  }

  "support default values" in {
    ScalaForms.defaultValuesForm.bindFromRequest(Map()).get must equalTo((42, "default text"))
    ScalaForms.defaultValuesForm.bindFromRequest(Map("name" -> Seq("another text"))).get must equalTo((42, "another text"))
    ScalaForms.defaultValuesForm.bindFromRequest(Map("pos" -> Seq("123"))).get must equalTo((123, "default text"))
    ScalaForms.defaultValuesForm.bindFromRequest(Map("pos" -> Seq("123"), "name" -> Seq("another text"))).get must equalTo((123, "another text"))

    val f1 = ScalaForms.defaultValuesForm.bindFromRequest(Map("pos" -> Seq("abc")))
    f1.errors must haveSize(1)
  }

  "support repeated values" in {
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"))).get must equalTo(("Kiki", Seq()))
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq("kiki@gmail.com"))).get must equalTo(("Kiki", Seq("kiki@gmail.com")))
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq("kiki@gmail.com"), "emails[1]" -> Seq("kiki@zen.com"))).get must equalTo(("Kiki", Seq("kiki@gmail.com", "kiki@zen.com")))
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq(), "emails[1]" -> Seq("kiki@zen.com"))).hasErrors must equalTo(true)
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[]" -> Seq("kiki@gmail.com"))).get must equalTo(("Kiki", Seq("kiki@gmail.com")))
    ScalaForms.repeatedForm.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[]" -> Seq("kiki@gmail.com", "kiki@zen.com"))).get must equalTo(("Kiki", Seq("kiki@gmail.com", "kiki@zen.com")))
  }

  "support repeated values with set" in {
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"))).get must equalTo(("Kiki", Set()))
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq("kiki@gmail.com"))).get must equalTo(("Kiki", Set("kiki@gmail.com")))
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq("kiki@gmail.com"), "emails[1]" -> Seq("kiki@zen.com"))).get must equalTo(("Kiki", Set("kiki@gmail.com", "kiki@zen.com")))
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[0]" -> Seq(), "emails[1]" -> Seq("kiki@zen.com"))).hasErrors must equalTo(true)
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[]" -> Seq("kiki@gmail.com"))).get must equalTo(("Kiki", Set("kiki@gmail.com")))
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[]" -> Seq("kiki@gmail.com", "kiki@zen.com"))).get must equalTo(("Kiki", Set("kiki@gmail.com", "kiki@zen.com")))
    ScalaForms.repeatedFormWithSet.bindFromRequest(Map("name" -> Seq("Kiki"), "emails[]" -> Seq("kiki@gmail.com", "kiki@gmail.com"))).get must equalTo(("Kiki", Set("kiki@gmail.com")))
  }

  "render a form with max 18 fields" in {
    ScalaForms.helloForm.bind(Map("name" -> "foo", "repeat" -> "1")).get.toString must equalTo("(foo,1,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)")
  }

  "render form using jodaDate" in {
    val dateForm = Form(("date" -> jodaDate))
    val data = Map("date" -> "2012-01-01")
    dateForm.bind(data).get mustEqual (new DateTime(2012, 1, 1, 0, 0))
  }

  "render form using jodaDate with format(30/1/2012)" in {
    val dateForm = Form(("date" -> jodaDate("dd/MM/yyyy")))
    val data = Map("date" -> "30/1/2012")
    dateForm.bind(data).get mustEqual (new DateTime(2012, 1, 30, 0, 0))
  }

  "render form using jodaLocalDate with format(30/1/2012)" in {
    val dateForm = Form(("date" -> jodaLocalDate("dd/MM/yyyy")))
    val data = Map("date" -> "30/1/2012")
    dateForm.bind(data).get mustEqual (new LocalDate(2012, 1, 30))
  }

  "reject input if it contains global errors" in {
    Form("value" -> nonEmptyText).withGlobalError("some.error")
      .bind(Map("value" -> "some value"))
      .errors.headOption must beSome.like {
        case error => error.message must equalTo("some.error")
      }
  }

  "find nested error on unbind" in {
    case class Item(text: String)
    case class Items(seq: Seq[Item])
    val itemForm = Form[Items](
      mapping(
        "seq" -> seq(
          mapping("text" -> nonEmptyText)(Item)(Item.unapply)
        )
      )(Items)(Items.unapply)
    )

    val filled = itemForm.fillAndValidate(Items(Seq(Item(""))))
    val result = filled.fold(
      errors => false,
      success => true
    )

    result should beFalse
  }

  "support boolean binding from json" in {
    ScalaForms.booleanForm.bind(Json.obj("accepted" -> "true")).get must beTrue
    ScalaForms.booleanForm.bind(Json.obj("accepted" -> "false")).get must beFalse
  }

  "reject boolean binding from an invalid json" in {
    val f = ScalaForms.booleanForm.bind(Json.obj("accepted" -> "foo"))
    f.errors must not be 'empty
  }
}

object ScalaForms {

  val booleanForm = Form("accepted" -> Forms.boolean)

  case class User(name: String, age: Int)

  val userForm = Form(
    mapping(
      "name" -> of[String].verifying(nonEmpty),
      "age" -> of[Int].verifying(min(0), max(100))
    )(User.apply)(User.unapply)
  )

  val loginForm = Form(
    tuple(
      "email" -> of[String],
      "password" -> of[Int]
    )
  )

  val defaultValuesForm = Form(
    tuple(
      "pos" -> default(number, 42),
      "name" -> default(text, "default text")
    )
  )

  val helloForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "repeat" -> number(min = 1, max = 100),
      "color" -> optional(text),
      "still works" -> optional(text),
      "1" -> optional(text),
      "2" -> optional(text),
      "3" -> optional(text),
      "4" -> optional(text),
      "5" -> optional(text),
      "6" -> optional(text),
      "7" -> optional(text),
      "8" -> optional(text),
      "9" -> optional(text),
      "10" -> optional(text),
      "11" -> optional(text),
      "12" -> optional(text),
      "13" -> optional(text),
      "14" -> optional(text)
    )
  )

  val repeatedForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "emails" -> list(nonEmptyText)
    )
  )

  val repeatedFormWithSet = Form(
    tuple(
      "name" -> nonEmptyText,
      "emails" -> set(nonEmptyText)
    )
  )

  val form = Form(
    "foo" -> Forms.text.verifying("first.digit", s => (s.headOption map { _ == '3' }) getOrElse false)
      .transform[Int](Integer.parseInt _, _.toString).verifying("number.42", _ < 42)
  )

  val emailForm = Form(
    tuple(
      "email" -> email,
      "name" -> of[String]
    )
  )

  val longNumberForm = Form("longNumber" -> longNumber(10, 42))

  val shortNumberForm = Form("shortNumber" -> shortNumber(10, 42))

  val byteNumberForm = Form("byteNumber" -> shortNumber(10, 42))
}
