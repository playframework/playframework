package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._

object JsonLensesSpec extends Specification {
  val article = JsObject(
    List(
      "title" -> JsString("Acme"),
      "author" -> JsObject(
        List(
          "firstname" -> JsString("Bugs"),
          "lastname" -> JsString("Bunny")
          )
        ),
      "tags" -> JsArray(
        List[JsValue](
          JsString("Awesome article"),
          JsString("Must read"),
          JsString("Playframework"),
          JsString("Rocks")
          )
        )
      )
    )

  "JSONLenses" should {
    "lens.self returns identity" in {
      article must equalTo(article)
    }

    "lens reads a sub-object" in {
      (JsLens \ "author" \ "firstname")(article) must equalTo(JsString("Bugs"))
    }

    "lens reads a sub-object with as" in {
      ((JsLens \ "author" \ "firstname").as[String]).get(article) must equalTo("Bugs")
    }

    "lens sets a sub-object with as" in {
      ((JsLens \ "author" \ "firstname").as[String]).set(article,"Koko") must equalTo(JsObject(
    List(
      "title" -> JsString("Acme"),
      "author" -> JsObject(
        List(
          "firstname" -> JsString("Koko"),
          "lastname" -> JsString("Bunny")
          )
        ),
      "tags" -> JsArray(
        List[JsValue](
          JsString("Awesome article"),
          JsString("Must read"),
          JsString("Playframework"),
          JsString("Rocks")
          )
        )
      )
    ))
    }

    "lens outside a sub-object with as throws exceptions" in {
      ((JsLens \ "author" \ "non-existant").as[String]).get(article) must throwAn[RuntimeException]
    }

    "lens outside a sub-object with asEither returns Left('some error')" in {
      ((JsLens \ "author" \ "non-existant").asEither[String]).get(article) must
        equalTo(Left[String, String]("Field non-existant not found"))
    }

    "lens sets a sub-object with asEither with Right(String)" in {
      (((JsLens \ "author" \ "login")
        .asEither[String]).set(article, Right[String,String]("bbunny"))) must
        equalTo(JsObject(
          List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs"),
                "lastname" -> JsString("Bunny"),
                "login" -> JsString("bbunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article"),
                JsString("Must read"),
                JsString("Playframework"),
                JsString("Rocks")
                )
              )
            )
          ))
    }

    "lens outside a sub-object with asOpt returns None" in {
      ((JsLens \ "author" \ "non-existant").asOpt[String]).get(article) must equalTo(None)
    }

    "lens sets a sub-object with asOpt with Some(String)" in {
      ((JsLens \ "author" \ "firstname").asOpt[String]).set(article,Some("Elmer")) must equalTo(JsObject(
    List(
      "title" -> JsString("Acme"),
      "author" -> JsObject(
        List(
          "firstname" -> JsString("Elmer"),
          "lastname" -> JsString("Bunny")
          )
        ),
      "tags" -> JsArray(
        List[JsValue](
          JsString("Awesome article"),
          JsString("Must read"),
          JsString("Playframework"),
          JsString("Rocks")
          )
        )
      )
    ))
    }

    "lens reads a sub-array" in {
      (JsLens \ "tags" at 0)(article) must equalTo(JsString("Awesome article"))
    }

    "compose lenses" in {
      val first =  JsLens \ "author"
      val second = JsLens \ "firstname"

      (first andThen second).apply(article) must equalTo(JsString("Bugs"))
    }

    "compose lenses and modify" in {
      val first =  JsLens \ "author"
      val second = JsLens \ "firstname"

      (first andThen second).apply(article, JsString("Daffy")) must equalTo(JsObject(
          List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Daffy"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article"),
                JsString("Must read"),
                JsString("Playframework"),
                JsString("Rocks")
                )
              )
            )
          )
        )
 
    }

    "set a value in a subobject with lenses" in {
      (JsLens \ "author" \ "firstname").set(article, JsString("Daffy")) must 
        equalTo(JsObject(
          List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Daffy"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article"),
                JsString("Must read"),
                JsString("Playframework"),
                JsString("Rocks")
                )
              )
            )
          )
        )
    }

    "set a value outside of an object" in {
      (JsLens \ "title" \ "foo")(article, JsString("bar")) must equalTo(JsObject(
        List(
          "title" -> JsObject(
            List(
              "foo" -> JsString("bar")
              )
            ),
          "author" -> JsObject(
            List(
              "firstname" -> JsString("Bugs"),
              "lastname" -> JsString("Bunny")
              )
            ),
          "tags" -> JsArray(
            List[JsValue](
              JsString("Awesome article"),
              JsString("Must read"),
              JsString("Playframework"),
              JsString("Rocks")
              )
            )
          )
        )
      )
    }

    "selectAll strings in article" in {
      JsLens.selectAll(article, a => a match {
        case JsString(_) => true
        case _ => false
        })
       .map(t => t._2) must equalTo(Seq(
         "Acme", "Bugs", "Bunny", "Awesome article", "Must read",
         "Playframework", "Rocks"
       ).map(t => JsString(t)))
    }

    "selectAll strings with max depth 1 in article" in {
      JsLens.selectAll(article, a => a match {
        case JsString(_) => true
        case _ => false
        }, 1)
       .map(t => t._2) must equalTo(Seq(
         "Acme"
       ).map(t => JsString(t)))
    }

    "prune author" in {
      JsLens.self \ "author" prune (article) must equalTo(
        JsObject(
          List(
            "title" -> JsString("Acme"),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article"),
                JsString("Must read"),
                JsString("Playframework"),
                JsString("Rocks")
                )
              )
            )
          )
      )
    }

    "mongo operator from lens" in {
      def mongoPrune(js: JsLens): JsValue = {
        val lens = (js).set(JsNull, JsNumber(1))
        lazy val mongoflatten: JsValue => JsObject = value => value match {
          case JsObject(elements) => 
            val next = mongoflatten(elements.head._2)
            if (next.fields.head._1 == "")
              JsObject(List(elements.head._1 -> next.fields.head._2))
            else
              JsObject(List(elements.head._1 + "." + next.fields.head._1 -> next.fields.head._2))

          //case JsArray(elements) => List(Left("0")) ++ mongoflatten(elements.head)
          case l => JsObject(List(""->l))
        }

        JsObject(List("$unset" -> mongoflatten(lens)))
      }

      val lens = JsLens.self \ "author" \ "firstname"

      Json.stringify(mongoPrune(lens)) must
        equalTo("""{"$unset":{"author.firstname":1}}""")
    }

    "prune author firstname" in {
      JsLens.self \ "author" \ "firstname" prune (article) must equalTo(
        JsObject(
          List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article"),
                JsString("Must read"),
                JsString("Playframework"),
                JsString("Rocks")
                )
              )
            )
          )
      )
    }

    "suffix all strings with a space" in {
      JsLens.self \\ (article, a => a match {
        case JsString(_) => true
        case _ => false
        }, a => a match {
          case JsString(s) => JsString(s + " ")
          case o => o
        }) must equalTo(JsObject(
          List(
            "title" -> JsString("Acme "),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs "),
                "lastname" -> JsString("Bunny ")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsString("Awesome article "),
                JsString("Must read "),
                JsString("Playframework "),
                JsString("Rocks ")
                )
              )
            )
          )
        )
    }

    "make all tags as subobjects" in {
      JsLens \ "tags" \\ (
        article,
        a => a match {
          case JsString(_) => true
          case _ => false
        }, a => a match {
          case JsString(s) => JsObject(List("name" -> JsString(s)))
          case o => o
        }) must equalTo(JsObject(
          List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsObject(List("name" -> JsString("Awesome article"))),
                JsObject(List("name" -> JsString("Must read"))),
                JsObject(List("name" -> JsString("Playframework"))),
                JsObject(List("name" -> JsString("Rocks")))
                )
              )
            )
          )
        )
    }

    "Prune a composed lens" in {
      val first = JsLens \ "tags" at 0
      val second = JsLens \ "name"

      val lens = first andThen second
      val alternativeArticles = JsObject(List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsObject(List("name" -> JsString("Awesome article"))),
                JsObject(List("name" -> JsString("Must read"))),
                JsObject(List("name" -> JsString("Playframework"))),
                JsObject(List("name" -> JsString("Rocks")))
                )
              )
            )
          )

      lens.prune(alternativeArticles) must equalTo(
        JsObject(List(
          "title" -> JsString("Acme"),
          "author" -> JsObject(
            List(
              "firstname" -> JsString("Bugs"),
              "lastname" -> JsString("Bunny")
              )
            ),
          "tags" -> JsArray(
            List[JsValue](
              JsObject(List()),
              JsObject(List("name" -> JsString("Must read"))),
              JsObject(List("name" -> JsString("Playframework"))),
              JsObject(List("name" -> JsString("Rocks")))
              )
            )
          )
        )
      )
    }

    "Prune subtags" in {
      val alternativeArticles = JsObject(List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsObject(List("name" -> JsString("Awesome article"))),
                JsObject(List("name" -> JsString("Must read"))),
                JsObject(List("name" -> JsString("Playframework"))),
                JsObject(List("name" -> JsString("Rocks")))
                )
              )
            )
          )

      JsLens.self \ "tags" \\ "name" prune alternativeArticles must equalTo(JsObject(List(
            "title" -> JsString("Acme"),
            "author" -> JsObject(
              List(
                "firstname" -> JsString("Bugs"),
                "lastname" -> JsString("Bunny")
                )
              ),
            "tags" -> JsArray(
              List[JsValue](
                JsObject(List()),
                JsObject(List()),
                JsObject(List()),
                JsObject(List())
                )
              )
            )
          ))
    }

//    "mask -whitelist- an object" in {
//      article &&& List("title", "author") > "tags" get must equalTo(JsUndefined)
//    }
//    "mask -blacklist- an object" in {
//      article ||| List("tags") > "tags" get must equalTo(JsUndefined)
//    }
  }

}

// vim: set ts=2 sw=2 ft=scala et:
