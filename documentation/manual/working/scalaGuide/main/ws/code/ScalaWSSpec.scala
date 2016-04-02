/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.ws.scalaws

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.asynchttpclient.AsyncHttpClientConfig

import play.api.{Mode, Environment}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc._
import play.api.test._

import java.io._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AfterAll

//#dependency
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.ExecutionContext

class Application @Inject() (ws: WSClient) extends Controller {

}
//#dependency


// #scalaws-person
case class Person(name: String, age: Int)
// #scalaws-person

/**
 * NOTE: the format here is because we cannot define a fake application in a new WithServer at once, as we run into a
 * JVM implementation issue.
 */
@RunWith(classOf[JUnitRunner])
class ScalaWSSpec extends PlaySpecification with Results with AfterAll {

  // #scalaws-context-injected
  class PersonService @Inject()(implicit context: ExecutionContext) {
    // ...
  }
  // #scalaws-context-injected
  val url = s"http://localhost:$testServerPort/"

  // #scalaws-context
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  // #scalaws-context

  val system = ActorSystem()
  implicit val materializer = ActorMaterializer()(system)

  def afterAll(): Unit = system.terminate()

  def withSimpleServer[T](block: WSClient => T): T = withServer {
    case _ => Action(Ok)
  }(block)

  def withServer[T](routes: (String, String) => Handler)(block: WSClient => T): T = {
    val app = GuiceApplicationBuilder().routes({
      case (method, path) => routes(method, path)
    }).build()
    running(TestServer(testServerPort, app))(block(app.injector.instanceOf[WSClient]))
  }

  def writeFile(file: File, content: String) = {
    file.getParentFile.mkdirs()
    val out = new FileWriter(file)
    try {
      out.write(content)
    } finally {
      out.close()
    }
  }

  /**
   * A source that produces a "large" result.
   *
   * In this case, 9 chunks, each containing abcdefghij repeated 100 times.
   */
  val largeSource: Source[ByteString, _] = {
    val source = Source.single(ByteString("abcdefghij" * 100))
    (1 to 9).foldLeft(source){(acc, _) => (acc ++ source)}
  }

  "WS" should {

    "allow making a request" in withSimpleServer { ws =>
      //#simple-holder
      val request: WSRequest = ws.url(url)
      //#simple-holder

      //#complex-holder
      val complexRequest: WSRequest =
        request.withHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)
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
        ws.url(url).withRequestTimeout(5000.millis).get()
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

      "post with multipart/form encoded body" in withServer {
          case("POST", "/") => Action(BodyParsers.parse.multipartFormData)(r => Ok(r.body.asFormUrlEncoded("key").head))
        } { ws =>
        import play.api.mvc.MultipartFormData._
        val response =
        //#multipart-encoded
        ws.url(url).post(Source.single(DataPart("key", "value")))
        //#multipart-encoded

        await(response).body must_== "value"
      }

      "post with multipart/form encoded body from a file" in withServer {
        case("POST", "/") => Action(BodyParsers.parse.multipartFormData){r =>
            val file = r.body.file("hello").head
          Ok(scala.io.Source.fromFile(file.ref.file).mkString)
        }
      } { ws =>
        val tmpFile = new File("/tmp/picture/tmpformuploaded")
        writeFile(tmpFile, "world")

        import play.api.mvc.MultipartFormData._
        val response =
        //#multipart-encoded2
        ws.url(url).post(Source(FilePart("hello", "hello.txt", Option("text/plain"), FileIO.fromFile(tmpFile)) :: DataPart("key", "value") :: List()))
        //#multipart-encoded2

        await(response).body must_== "world"
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
        case ("GET", "/") => Action(Ok.chunked(largeSource))
      } { ws =>
        //#stream-count-bytes
        // Make the request
        val futureResponse: Future[StreamedResponse] =
          ws.url(url).withMethod("GET").stream()

        val bytesReturned: Future[Long] = futureResponse.flatMap {
          res =>
            // Count the number of bytes returned
            res.body.runWith(Sink.fold[Long, ByteString](0L){ (total, bytes) =>
              total + bytes.length
            })
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000l
      }

      "stream to a file" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeSource))
      } { ws =>
        val file = File.createTempFile("stream-to-file-", ".txt")
        try {
          //#stream-to-file
          // Make the request
          val futureResponse: Future[StreamedResponse] =
            ws.url(url).withMethod("GET").stream()

          val downloadedFile: Future[File] = futureResponse.flatMap {
            res =>
              val outputStream = new FileOutputStream(file)

              // The sink that writes to the output stream
              val sink = Sink.foreach[ByteString] { bytes =>
                outputStream.write(bytes.toArray)
              }

              // materialize and run the stream
              res.body.runWith(sink).andThen {
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
        case ("GET", "/") => Action(Ok.chunked(largeSource))
      } { ws =>
          //#stream-to-result
          def downloadFile = Action.async {

            // Make the request
            ws.url(url).withMethod("GET").stream().map {
              case StreamedResponse(response, body) =>

                // Check that the response was successful
                if (response.status == 200) {

                  // Get the content type
                  val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
                    .getOrElse("application/octet-stream")

                  // If there's a content length, send that, otherwise return the body chunked
                  response.headers.get("Content-Length") match {
                    case Some(Seq(length)) =>
                      Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
                    case _ =>
                      Ok.chunked(body).as(contentType)
                  }
                } else {
                  BadGateway
                }
            }
          }
          //#stream-to-result
          val file = File.createTempFile("stream-to-file-", ".txt")
          await(
            downloadFile(FakeRequest())
              .flatMap(_.body.dataStream.runFold(0l)((t, b) => t + b.length))
          ) must_== 10000l
          file.delete()
      }

      "stream when request is a PUT" in withServer {
        case ("PUT", "/") => Action(Ok.chunked(largeSource))
      } { ws =>
        //#stream-put
        val futureResponse: Future[StreamedResponse] =
          ws.url(url).withMethod("PUT").withBody("some body").stream()
        //#stream-put

        val bytesReturned: Future[Long] = futureResponse.flatMap {
          res =>
            res.body.runWith(Sink.fold[Long, ByteString](0L){ (total, bytes) =>
              total + bytes.length
            })
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000l
      }


    "stream request body" in withServer {
        case ("PUT", "/") => Action(Ok(""))
      } { ws =>
        def largeImageFromDB: Source[ByteString, _] = largeSource
        //#scalaws-stream-request
        val wsResponse: Future[WSResponse] = ws.url(url)
          .withBody(StreamedBody(largeImageFromDB)).execute("PUT")
        //#scalaws-stream-request
        await(wsResponse).status must_== 200
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
      implicit val sslClient = AhcWSClient()
      // close with sslClient.close() when finished with client
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

      // If running in Play, environment should be injected
      val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

      //#ws-custom-client
      import com.typesafe.config.ConfigFactory
      import play.api._
      import play.api.libs.ws._

      val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
        """
          |ws.followRedirects = true
        """.stripMargin))

      val parser = new WSConfigParser(configuration, environment)
      val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
      val builder = new AhcConfigBuilder(config)
      val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
        override def initChannel(channel: io.netty.channel.Channel): Unit = {
          channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("debug"))
        }
      }
      val ahcBuilder = builder.configure()
      ahcBuilder.setHttpAdditionalChannelInitializer(logging)
      val ahcConfig = ahcBuilder.build()
      val wsClient = new AhcWSClient(ahcConfig)
      //#ws-custom-client

      //#close-client
      wsClient.close()
      //#close-client

      ok
    }

    "grant access to the underlying client" in withSimpleServer { ws =>
      //#underlying
      import org.asynchttpclient.AsyncHttpClient

      val client: AsyncHttpClient = ws.underlying
      //#underlying

      ok
    }

    "use logging" in withSimpleServer { ws =>
      // #curl-logger-filter
      ws.url(s"http://localhost:$testServerPort")
        .withRequestFilter(AhcCurlRequestLogger())
        .withBody(Map("param1" -> Seq("value1")))
        .put(Map("key" -> Seq("value")))
      // #curl-logger-filter

      ok
    }

  }
}
