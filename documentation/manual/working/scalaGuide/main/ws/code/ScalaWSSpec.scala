/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.ws.scalaws

import play.api.test._

import java.io._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

//#dependency
import javax.inject.Inject
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.ws._

class Application @Inject() (ws: WSClient) extends Controller {

}
//#dependency


// #scalaws-person
case class Person(name: String, age: Int)
// #scalaws-person

/**
 * NOTE: the format here is because we cannot define FakeApplication in a new WithServer at once, as we run into a
 * JVM implementation issue.
 */
@RunWith(classOf[JUnitRunner])
class ScalaWSSpec extends PlaySpecification with Results {

  import scala.concurrent.ExecutionContext.global

  val url = s"http://localhost:$testServerPort/"

  // #scalaws-context
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  // #scalaws-context

  def withSimpleServer[T](block: WSClient => T): T = withServer {
    case _ => Action(Ok)
  }(block)

  def withServer[T](routes: (String, String) => Handler)(block: WSClient => T): T = {
    val app = FakeApplication(withRoutes = {
      case (method, path) => routes(method, path)
    })
    running(TestServer(testServerPort, app))(block(app.injector.instanceOf[WSClient]))
  }

  /**
   * An enumerator that produces a large result.
   *
   * In this case, 10 chunks, each containing abcdefghij repeated 100 times.
   */
  val largeEnumerator = {
    val bytes = ("abcdefghij" * 100).getBytes("utf-8")
    import play.api.libs.iteratee._
    Enumerator.unfold(10) {
      case 0 => None
      case seq => Some((seq - 1, bytes))
    }
  }

  "WS" should {

    "allow making a request" in withSimpleServer { ws =>
      //#simple-holder
      val request: WSRequest = ws.url(url)
      //#simple-holder

      //#complex-holder
      val complexRequest: WSRequest =
        request.withHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000)
          .withQueryString("search" -> "play")
      //#complex-holder

      //#holder-get
      val futureResponse: Future[WSResponse] = complexRequest.get()
      //#holder-get

      await(futureResponse).status must_== 200
    }

    "allow making an authenticated request" in withSimpleServer { ws =>
      val user = "user"
      val password = "password"

      val response =
        //#auth-request
        ws.url(url).withAuth(user, password, WSAuthScheme.BASIC).get()
        //#auth-request

      await(response).status must_== 200
    }

    "allow following redirects" in withSimpleServer { ws =>
      val response =
        //#redirects
        ws.url(url).withFollowRedirects(true).get()
        //#redirects

      await(response).status must_== 200
    }

    "allow setting a query string" in withSimpleServer { ws =>
      val response =
        //#query-string
        ws.url(url).withQueryString("paramKey" -> "paramValue").get()
        //#query-string

      await(response).status must_== 200
    }

    "allow setting headers" in withSimpleServer { ws =>
      val response =
        //#headers
        ws.url(url).withHeaders("headerKey" -> "headerValue").get()
        //#headers

      await(response).status must_== 200
    }

    "allow setting the content type" in withSimpleServer { ws =>
      val xmlString = "<foo></foo>"
      val response =
        //#content-type
        ws.url(url).withHeaders("Content-Type" -> "application/xml").post(xmlString)
        //#content-type

      await(response).status must_== 200
    }

    "allow setting the virtual host" in withSimpleServer { ws =>
      val response =
        //#virtual-host
        ws.url(url).withVirtualHost("192.168.1.1").get()
        //#virtual-host

      await(response).status must_== 200
    }

    "allow setting the request timeout" in withSimpleServer { ws =>
      val response =
        //#request-timeout
        ws.url(url).withRequestTimeout(5000).get()
        //#request-timeout

      await(response).status must_== 200
    }

    "when posting data" should {

      "post with form url encoded body" in withServer {
        case ("POST", "/") => Action(BodyParsers.parse.urlFormEncoded)(r => Ok(r.body("key").head))
      } { ws =>
        val response =
          //#url-encoded
          ws.url(url).post(Map("key" -> Seq("value")))
          //#url-encoded

        await(response).body must_== "value"
      }

      "post with JSON body" in  withServer {
        case ("POST", "/") => Action(BodyParsers.parse.json)(r => Ok(r.body))
      } { ws =>
        // #scalaws-post-json
        import play.api.libs.json._
        val data = Json.obj(
          "key1" -> "value1",
          "key2" -> "value2"
        )
        val futureResponse: Future[WSResponse] = ws.url(url).post(data)
        // #scalaws-post-json

        await(futureResponse).json must_== data
      }

      "post with XML data" in withServer {
        case ("POST", "/") => Action(BodyParsers.parse.xml)(r => Ok(r.body))
      } { ws =>
        // #scalaws-post-xml
        val data = <person>
          <name>Steve</name>
          <age>23</age>
        </person>
        val futureResponse: Future[WSResponse] = ws.url(url).post(data)
        // #scalaws-post-xml

        await(futureResponse).xml must_== data
      }
    }

    "when processing a response" should {

      "handle as JSON" in withServer {
        case ("GET", "/") => Action {
          import play.api.libs.json._
          implicit val personWrites = Json.writes[Person]
          Ok(Json.obj("person" -> Person("Steve", 23)))
        }
      } { ws =>
        // #scalaws-process-json
        val futureResult: Future[String] = ws.url(url).get().map {
          response =>
            (response.json \ "person" \ "name").as[String]
        }
        // #scalaws-process-json

        await(futureResult) must_== "Steve"
      }

      "handle as JSON with an implicit" in withServer {
        case ("GET", "/") => Action {
          import play.api.libs.json._
          implicit val personWrites = Json.writes[Person]
          Ok(Json.obj("person" -> Person("Steve", 23)))
        }
      } { ws =>
        // #scalaws-process-json-with-implicit
        import play.api.libs.json._

        implicit val personReads = Json.reads[Person]

        val futureResult: Future[JsResult[Person]] = ws.url(url).get().map {
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

      "handle as XML" in withServer {
        case ("GET", "/") =>
          Action {
            Ok(
              """<?xml version="1.0" encoding="utf-8"?>
                |<wrapper><message status="OK">Hello</message></wrapper>
              """.stripMargin).as("text/xml")
          }
      } { ws =>
        // #scalaws-process-xml
        val futureResult: Future[scala.xml.NodeSeq] = ws.url(url).get().map {
          response =>
            response.xml \ "message"
        }
        // #scalaws-process-xml
        await(futureResult).text must_== "Hello"
      }

      "handle as stream" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeEnumerator))
      } { ws =>
        //#stream-count-bytes
        import play.api.libs.iteratee._

        // Make the request
        val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
          ws.url(url).getStream()

        val bytesReturned: Future[Long] = futureResponse.flatMap {
          case (headers, body) =>
            // Count the number of bytes returned
            body |>>> Iteratee.fold(0l) { (total, bytes) =>
              total + bytes.length
            }
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000l
      }

      "stream to a file" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeEnumerator))
      } { ws =>
        val file = File.createTempFile("stream-to-file-", ".txt")
        try {
          //#stream-to-file
          import play.api.libs.iteratee._

          // Make the request
          val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
            ws.url(url).getStream()

          val downloadedFile: Future[File] = futureResponse.flatMap {
            case (headers, body) =>
              val outputStream = new FileOutputStream(file)

              // The iteratee that writes to the output stream
              val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
                outputStream.write(bytes)
              }

              // Feed the body into the iteratee
              (body |>>> iteratee).andThen {
                case result =>
                  // Close the output stream whether there was an error or not
                  outputStream.close()
                  // Get the result or rethrow the error
                  result.get
              }.map(_ => file)
          }
          //#stream-to-file
          await(downloadedFile) must_== file

        } finally {
          file.delete()
        }
      }

      "stream to a result" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeEnumerator))
      } { ws =>
        val file = File.createTempFile("stream-to-file-", ".txt")
        try {
          //#stream-to-result
          def downloadFile = Action.async {

            // Make the request
            ws.url(url).getStream().map {
              case (response, body) =>

                // Check that the response was successful
                if (response.status == 200) {

                  // Get the content type
                  val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
                    .getOrElse("application/octet-stream")

                  // If there's a content length, send that, otherwise return the body chunked
                  response.headers.get("Content-Length") match {
                    case Some(Seq(length)) =>
                      Ok.feed(body).as(contentType).withHeaders("Content-Length" -> length)
                    case _ =>
                      Ok.chunked(body).as(contentType)
                  }
                } else {
                  BadGateway
                }
            }
          }
          //#stream-to-result
          import play.api.libs.iteratee._
          await(
            downloadFile(FakeRequest())
              .flatMap(_.body &> Results.dechunk |>>> Iteratee.fold(0l)((t, b) => t + b.length))
          ) must_== 10000l

        } finally {
          file.delete()
        }
      }

      "stream when request is a PUT" in withServer {
        case ("PUT", "/") => Action(Ok.chunked(largeEnumerator))
      } { ws =>
        import play.api.libs.iteratee._

        //#stream-put
        val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
          ws.url(url).withMethod("PUT").withBody("some body").stream()
        //#stream-put

        val bytesReturned: Future[Long] = futureResponse.flatMap {
          case (headers, body) =>
            body |>>> Iteratee.fold(0l) { (total, bytes) =>
              total + bytes.length
            }
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000l
      }
    }

    "work with for comprehensions" in withServer {
      case ("GET", "/one") =>
        Action {
          Ok(s"http://localhost:$testServerPort/two")
        }
      case ("GET", "/two") =>
        Action {
          Ok(s"http://localhost:$testServerPort/three")
        }
      case ("GET", "/three") =>
        Action {
          Ok("finished!")
        }
    } { ws =>
      val urlOne = s"http://localhost:$testServerPort/one"
      val exceptionUrl = s"http://localhost:$testServerPort/fallback"
      // #scalaws-forcomprehension
      val futureResponse: Future[WSResponse] = for {
        responseOne <- ws.url(urlOne).get()
        responseTwo <- ws.url(responseOne.body).get()
        responseThree <- ws.url(responseTwo.body).get()
      } yield responseThree

      futureResponse.recover {
        case e: Exception =>
          val exceptionData = Map("error" -> Seq(e.getMessage))
          ws.url(exceptionUrl).post(exceptionData)
      }
      // #scalaws-forcomprehension

      await(futureResponse).body must_== "finished!"
    }

    "map to async result" in withSimpleServer { ws =>
      //#async-result
      def wsAction = Action.async {
        ws.url(url).get().map { response =>
          Ok(response.body)
        }
      }
      status(wsAction(FakeRequest())) must_== OK
      //#async-result
    }

    "allow working with clients directly" in withSimpleServer { ws =>

      //#implicit-client
      import play.api.libs.ws.ning._
      import com.ning.http.client.AsyncHttpClientConfig

      val clientConfig = new NingWSClientConfig()
      val secureDefaults: AsyncHttpClientConfig = new NingAsyncHttpClientConfigBuilder(clientConfig).build()
      // You can directly use the builder for specific options once you have secure TLS defaults...
      val builder = new AsyncHttpClientConfig.Builder(secureDefaults)
      builder.setCompressionEnforced(true)
      val secureDefaultsWithSpecificOptions: AsyncHttpClientConfig = builder.build()
      implicit val sslClient = new NingWSClient(secureDefaultsWithSpecificOptions)

      val response = WS.clientUrl(url).get()
      //#implicit-client
      await(response).status must_== OK

      {
        //#direct-client
        val response = sslClient.url(url).get()
        //#direct-client
        await(response).status must_== OK
      }

      sslClient.close()
      ok
    }

    "allow using pair magnets" in withSimpleServer { ws =>
      //#pair-magnet
      object PairMagnet {
        implicit def fromPair(pair: (WSClient, java.net.URL)) =
          new WSRequestMagnet {
            def apply(): WSRequest = {
              val (client, netUrl) = pair
              client.url(netUrl.toString)
            }
          }
      }

      import scala.language.implicitConversions
      import PairMagnet._

      val exampleURL = new java.net.URL(url)
      val response = WS.url(ws -> exampleURL).get()
      //#pair-magnet

      await(response).status must_== OK
    }

    "allow programmatic configuration" in new WithApplication() {

      //#programmatic-config
      import com.typesafe.config.ConfigFactory
      import play.api._
      import play.api.libs.ws._
      import play.api.libs.ws.ning._

      val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
        """
          |ws.followRedirects = true
        """.stripMargin))

      // If running in Play, environment should be injected
      val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

      val parser = new WSConfigParser(configuration, environment)
      val config = new NingWSClientConfig(wsClientConfig = parser.parse())
      val builder = new NingAsyncHttpClientConfigBuilder(config)
      //#programmatic-config

      ok
    }

    "grant access to the underlying client" in withSimpleServer { ws =>
      //#underlying
      import com.ning.http.client.AsyncHttpClient

      val client: AsyncHttpClient = ws.underlying
      //#underlying

      ok
    }

  }
}


