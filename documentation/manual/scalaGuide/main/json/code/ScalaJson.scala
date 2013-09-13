package scalaguide.json

import org.specs2.mutable.Specification
import play.api.data.validation.ValidationError

object ScalaJsonSpec extends Specification {

  "Scala JSON" should {
    "parse json" in {
      //#parse-json
      import play.api.libs.json._

      val json: JsValue = Json.parse("""
      {
        "user": {
          "name" : "toto",
          "age" : 25,
          "email" : "toto@jmail.com",
          "isAlive" : true,
          "friend" : {
            "name" : "tata",
            "age" : 20,
            "email" : "tata@coldmail.com"
          }
        }
      }
      """)
      //#parse-json

      (json \ "user" \ "friend" \ "name") must_== JsString("tata")

      {
        //#serialise-json
        val jsonString: String = Json.stringify(json)
        //#serialise-json

        jsonString must contain("\"toto@jmail.com\"")
      }

      {
        //#traverse-path
        val name: JsValue = json \ "user" \ "name"

        name === JsString("toto")
        //#traverse-path
      }

      {
        //#recursive-traverse-path
        val emails: Seq[JsValue] = json \ "user" \\ "email"

        emails === Seq(JsString("toto@jmail.com"), JsString("tata@coldmail.com"))
        //#recursive-traverse-path
      }

      {
        //#as-method
        val name: String = (json \ "user" \ "name").as[String]

        name === "toto"
        //#as-method
      }

      {
        //#as-opt
        val maybeName: Option[String] = (json \ "user" \ "name").asOpt[String]

        maybeName === Some("toto")
        //#as-opt
      }

      {
        //#validate-success
        val jsresult: JsResult[String] = (json \ "user" \ "name").validate[String]

        jsresult === JsSuccess("toto")
        //#validate-success
      }

      {
        //#validate-failure
        val nameXXX = (json \ "user" \ "nameXXX").validate[String]

        nameXXX === JsError(List((JsPath, List(ValidationError("error.expected.jsstring")))))
        //#validate-failure
      }

      {
        //#validate-compose
        val nameAndEmail: JsResult[(String, String)] = for {
          name <- (json \ "user" \ "name").validate[String]
          email <- (json \ "user" \ "email").validate[String]
        } yield (name, email)

        nameAndEmail must_== JsSuccess(("toto", "toto@jmail.com"))
        //#validate-compose
      }

      {
        //#recursive-as
        val emails: Seq[String] = (json \ "user" \\ "email").map(_.as[String])

        emails === Seq("toto@jmail.com", "tata@coldmail.com")
        //#recursive-as
      }


    }

    "allow constructing json using case classes" in {
      //#construct-json-case-class
      import play.api.libs.json._

      JsObject(Seq(
        "users" -> JsArray(Seq(
          JsObject(Seq(
            "name" -> JsString("Bob"),
            "age" -> JsNumber(31),
            "email" -> JsString("bob@gmail.com")
          )),
          JsObject(Seq(
            "name" -> JsString("Kiki"),
            "age" -> JsNumber(25),
            "email" -> JsNull
          ))
        ))
      ))
      //#construct-json-case-class
        .\("users").\\("name").head must_== JsString("Bob")
    }

    "allow constructing json using the DSL" in {
      //#construct-json-dsl
      import play.api.libs.json._

      Json.obj(
        "users" -> Json.arr(
          Json.obj(
            "name" -> "Bob",
            "age" -> 31,
            "email" -> "bob@gmail.com"
          ),
          Json.obj(
            "name" -> "Kiki",
            "age" -> 25,
            "email" -> JsNull
          )
        )
      )
      //#construct-json-dsl
        .\("users").\\("name").head must_== JsString("Bob")
    }

    "allow converting a simple type" in {
      //#convert-simple-type
      import play.api.libs.json._

      val jsonNumber = Json.toJson(4)

      jsonNumber === JsNumber(4)
      //#convert-simple-type
    }

    "allow converting a sequence" in {
      //#convert-seq
      import play.api.libs.json._

      val jsonArray = Json.toJson(Seq(1, 2, 3, 4))

      jsonArray === Json.arr(1, 2, 3, 4)
      //#convert-seq
    }

    "allow converting a hetrogeneous sequence" in {
      //#convert-hetro-seq
      import play.api.libs.json._

      val jsonArray = Json.toJson(Seq(
        Json.toJson(1), Json.toJson("Bob"), Json.toJson(3), Json.toJson(4)
      ))

      jsonArray === Json.arr(1, "Bob", 3, 4)
      //#convert-hetro-seq
    }
  }

}
