package scalaguide.ws.scalaws

import play.api.libs.ws._
import scala.concurrent.Future

import play.api.mvc._
import play.api.test._

import java.io._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

// #scalaws-person
case class Person(name: String, age: Int)

// #scalaws-person

/**
 * NOTE: the format here is because we cannot define FakeApplication in a new WithServer at once, as we run into a
 * JVM implementation issue.
 */
@RunWith(classOf[JUnitRunner])
class ScalaWSSpec extends PlaySpecification with Results {

  val url = "http://localhost:3333/"

  // #scalaws-context
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  // #scalaws-context

  "WS" should {

    "when posting data" should {

      "post with JSON body" should {

        val fa = FakeApplication(withRoutes = {
          case ("POST", "/") =>
            import play.api.libs.json._

            Action(BodyParsers.parse.json) {
              request =>
                val body: JsValue = request.body
                val value1: String = (body \ "key1").as[String]
                val value2: String = (body \ "key2").as[String]

                (value1, value2) match {
                  case ("value1", "value2") =>
                    Ok("valid json!")
                  case _ =>
                    BadRequest("invalid json: " + body)
                }
            }
        })

        "work" in new WithServer(fa, 3333) {
          // #scalaws-post-json
          import play.api.libs.json._
          val data = Json.obj(
            "key1" -> "value1",
            "key2" -> "value2"
          )
          val futureResponse: Future[Response] = WS.url(url).post(data)
          // #scalaws-post-json

          val actual = await(futureResponse)
          actual.body must beEqualTo("valid json!")
        }
      }

      "post with XML data" should {
        val fa = FakeApplication(withRoutes = {
          case ("POST", "/") =>
            Action(BodyParsers.parse.xml) {
              request =>
                (request.body \\ "name" headOption).map(_.text).map {
                  name =>
                    Ok("valid XML!")
                }.getOrElse {
                  BadRequest("Missing parameter [name]")
                }
            }
        })

        "work" in new WithServer(fa, 3333) {
          // #scalaws-post-xml
          val data = <person>
            <name>Steve</name>
            <age>23</age>
          </person>
          val futureResponse: Future[Response] = WS.url(url).post(data)
          // #scalaws-post-xml
          val actual = await(futureResponse)
          actual.body must beEqualTo("valid XML!")
        }
      }
    }

    "process a response" should {

      "as JSON" should {
        val fa = FakeApplication(withRoutes = {
          case ("GET", "/") =>
            import play.api.libs.json._
            implicit val personWrites = Json.writes[Person]

            Action {
              val steve = Person("Steve", 23)
              Ok(Json.obj("person" -> steve))
            }
        })

        "work" in new WithServer(fa, 3333) {
          // #scalaws-process-json
          val futureResult: Future[String] = WS.url(url).get().map {
            response =>
              (response.json \ "person" \ "name").as[String]
          }
          // #scalaws-process-json
          val actual = await(futureResult)
          actual must beEqualTo("Steve")
        }
      }

      "as JSON with an implicit" should {
        val fa = FakeApplication(withRoutes = {
          case ("GET", "/") =>
            import play.api.libs.json._
            implicit val personWrites = Json.writes[Person]

            Action {
              val steve = Person("Steve", 23)
              Ok(Json.obj("person" -> steve))
            }
        })

        "work" in new WithServer(fa, 3333) {
          // #scalaws-process-json-with-implicit
          import play.api.libs.json._
          import play.api.libs.functional.syntax._

          implicit val personReads: Reads[Person] = (
            (__ \ "name").read[String]
              and (__ \ "age").read[Int]
            )(Person)

          val futureResult: Future[JsResult[Person]] = WS.url(url).get().map {
            response => (response.json \ "person").validate[Person]
          }
          // #scalaws-process-json-with-implicit

          val actual = await(futureResult)
          actual.asOpt must beSome[Person].which {
            person =>
              person.age must beEqualTo(23)
              person.name must beEqualTo("Steve")
          }
        }
      }

      "as XML" should {
        val fa = FakeApplication(withRoutes = {
          case ("GET", "/") =>
            Action {
              Ok(
                """<?xml version="1.0" encoding="utf-8"?>
                  |<wrapper><message status="OK">Hello</message></wrapper>
                """.stripMargin).as("text/xml")
            }
        })

        "work" in new WithServer(fa, 3333) {
          // #scalaws-process-xml
          val futureResult: Future[scala.xml.NodeSeq] = WS.url(url).get().map {
            response =>
              response.xml \ "message"
          }
          // #scalaws-process-xml
          val actual = await(futureResult)
          actual.text must beEqualTo("Hello")
        }
      }

      "with a large file" should {
        val fa = FakeApplication(withRoutes = {
          case ("GET", "/") =>
            import play.api.libs.iteratee._

            Action {
              // Create an input stream of lots of null bytes.
              val inputStream: InputStream = new InputStream() {
                private var count = 0
                private val numBytes = 4096

                def read(): Int = {
                  count = count + 1
                  if (count > numBytes) -1 else 0
                }
              }
              Ok.chunked(Enumerator.fromStream(inputStream))
            }
        })

        "work" in new WithServer(fa, 3333) {
          val file = File.createTempFile("scalaws", "")
          try {
            // #scalaws-fileupload
            import play.api.libs.iteratee._

            def fromStream(stream: OutputStream): Iteratee[Array[Byte], Unit] = Cont {
              case e@Input.EOF =>
                stream.close()
                Done((), e)
              case Input.El(data) =>
                stream.write(data)
                fromStream(stream)
              case Input.Empty =>
                fromStream(stream)
            }

            val outputStream: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
            val futureResponse: Future[Unit] = WS.url(url).withTimeout(3000).get {
              headers =>
                fromStream(outputStream)
            }.flatMap(_.run)
            // #scalaws-fileupload

            await(futureResponse, 4000)
            file.exists() must beTrue
            file.length() must beEqualTo(4096)
          } finally {
            file.delete()
          }
        }
      }

      "with for comprehensions" should {
        val fa = FakeApplication(withRoutes = {
          case ("GET", "/one") =>
            Action {
              Ok("http://localhost:3333/two")
            }
          case ("GET", "/two") =>
            Action {
              Ok("http://localhost:3333/three")
            }
          case ("GET", "/three") =>
            Action {
              Ok("finished!")
            }
        })

        "work" in new WithServer(fa, 3333) {
          val urlOne = "http://localhost:3333/one"
          val exceptionUrl = "http://localhost:3333/fallback"
          // #scalaws-forcomprehension
          val futureResponse: Future[Response] = for {
            responseOne <- WS.url(urlOne).get()
            responseTwo <- WS.url(responseOne.body).get()
            responseThree <- WS.url(responseTwo.body).get()
          } yield responseThree

          futureResponse.recover {
            case e: Exception =>
              val exceptionData = Map("error" -> Seq(e.getMessage))
              WS.url(exceptionUrl).post(exceptionData)
          }
          // #scalaws-forcomprehension

          val actual = await(futureResponse)
          actual.body must beEqualTo("finished!")
        }
      }
    }
  }

}


