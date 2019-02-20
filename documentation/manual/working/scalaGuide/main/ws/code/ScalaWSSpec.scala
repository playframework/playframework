/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.ws.scalaws

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc._
import play.api.test._
import java.io._
import java.net.URL

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AfterAll
import play.api.libs.concurrent.Futures
import play.api.libs.json.JsValue

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

class Application @Inject()(ws: WSClient) extends Controller {}
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
  class PersonService @Inject()(ec: ExecutionContext) {
    // ...
  }
  // #scalaws-context-injected
  val url = s"http://localhost:$testServerPort/"

  val system                = ActorSystem()
  implicit val materializer = ActorMaterializer()(system)

  def afterAll(): Unit = system.terminate()

  def withSimpleServer[T](block: WSClient => T): T =
    withServer {
      case _ => Action(Ok)
    }(block)

  def withServer[T](routes: (String, String) => Handler)(block: WSClient => T): T = {
    val app = GuiceApplicationBuilder()
      .configure("play.http.filters" -> "play.api.http.NoHttpFilters")
      .appRoutes(a => {
        case (method, path) => routes(method, path)
      })
      .build()
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
    (1 to 9).foldLeft(source) { (acc, _) =>
      (acc ++ source)
    }
  }

  "WS" should {
    import scala.concurrent.ExecutionContext.Implicits.global

    "allow making a request" in withSimpleServer { ws =>
      //#simple-holder
      val request: WSRequest = ws.url(url)
      //#simple-holder

      //#complex-holder
      val complexRequest: WSRequest =
        request
          .addHttpHeaders("Accept" -> "application/json")
          .addQueryStringParameters("search" -> "play")
          .withRequestTimeout(10000.millis)
      //#complex-holder

      //#holder-get
      val futureResponse: Future[WSResponse] = complexRequest.get()
      //#holder-get

      await(futureResponse).status must_== 200
    }

    "allow making an authenticated request" in withSimpleServer { ws =>
      val user     = "user"
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
        ws.url(url).addQueryStringParameters("paramKey" -> "paramValue").get()
      //#query-string

      await(response).status must_== 200
    }

    "allow setting headers" in withSimpleServer { ws =>
      val response =
        //#headers
        ws.url(url).addHttpHeaders("headerKey" -> "headerValue").get()
      //#headers

      await(response).status must_== 200
    }

    "allow setting the content type" in withSimpleServer { ws =>
      val xmlString = "<foo></foo>"
      val response  =
        //#content-type
        ws.url(url)
          .addHttpHeaders("Content-Type" -> "application/xml")
          .post(xmlString)
      //#content-type

      await(response).status must_== 200
    }

    "allow setting the cookie" in withSimpleServer { ws =>
      val response =
        //#cookie
        ws.url(url).addCookies(DefaultWSCookie("cookieName", "cookieValue")).get()
      //#cookie

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
        case ("POST", "/") => Action(BodyParsers.parse.formUrlEncoded)(r => Ok(r.body("key").head))
        case other         => Action { NotFound }
      } { ws =>
        val response =
          //#url-encoded
          ws.url(url).post(Map("key" -> Seq("value")))
        //#url-encoded

        await(response).body must_== "value"
      }

      "post with multipart/form encoded body" in withServer {
        case ("POST", "/") => Action(BodyParsers.parse.multipartFormData)(r => Ok(r.body.asFormUrlEncoded("key").head))
        case other         => Action { NotFound }
      } { ws =>
        import play.api.mvc.MultipartFormData._
        val response =
          //#multipart-encoded
          ws.url(url).post(Source.single(DataPart("key", "value")))
        //#multipart-encoded

        await(response).body must_== "value"
      }

      "post with multipart/form encoded body from a file" in withServer {
        case ("POST", "/") =>
          Action(BodyParsers.parse.multipartFormData) { r =>
            val file = r.body.file("hello").head
            Ok(scala.io.Source.fromFile(file.ref).mkString)
          }
        case other => Action { NotFound }
      } { ws =>
        val tmpFile = new File("/tmp/picture/tmpformuploaded")
        writeFile(tmpFile, "world")

        import play.api.mvc.MultipartFormData._
        val response =
          //#multipart-encoded2
          ws.url(url)
            .post(
              Source(
                FilePart("hello", "hello.txt", Option("text/plain"), FileIO.fromPath(tmpFile.toPath)) :: DataPart(
                  "key",
                  "value"
                ) :: List()
              )
            )
        //#multipart-encoded2

        await(response).body must_== "world"
      }

      "post with JSON body" in withServer {
        case ("POST", "/") => Action(BodyParsers.parse.json)(r => Ok(r.body))
        case other         => Action { NotFound }
      } { ws =>
        // #scalaws-post-json
        import play.api.libs.json._
        val data = Json.obj(
          "key1" -> "value1",
          "key2" -> "value2"
        )
        val futureResponse: Future[WSResponse] = ws.url(url).post(data)
        // #scalaws-post-json

        await(futureResponse).body[JsValue] must_== data
      }

      "post with XML data" in withServer {
        case ("POST", "/") => Action(BodyParsers.parse.xml)(r => Ok(r.body))
        case other         => Action { NotFound }
      } { ws =>
        // #scalaws-post-xml
        val data                               = <person>
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
        case ("GET", "/") =>
          Action {
            import play.api.libs.json._
            implicit val personWrites = Json.writes[Person]
            Ok(Json.obj("person" -> Person("Steve", 23)))
          }
        case other => Action { NotFound }
      } { ws =>
        // #scalaws-process-json
        val futureResult: Future[String] = ws.url(url).get().map { response =>
          (response.json \ "person" \ "name").as[String]
        }
        // #scalaws-process-json

        await(futureResult) must_== "Steve"
      }

      "handle as JSON with an implicit" in withServer {
        case ("GET", "/") =>
          Action {
            import play.api.libs.json._
            implicit val personWrites = Json.writes[Person]
            Ok(Json.obj("person" -> Person("Steve", 23)))
          }
        case other => Action { NotFound }
      } { ws =>
        // #scalaws-process-json-with-implicit
        import play.api.libs.json._

        implicit val personReads = Json.reads[Person]

        val futureResult: Future[JsResult[Person]] = ws.url(url).get().map { response =>
          (response.json \ "person").validate[Person]
        }
        // #scalaws-process-json-with-implicit

        val actual = await(futureResult)
        actual.asOpt must beSome[Person].which { person =>
          person.age must beEqualTo(23)
          person.name must beEqualTo("Steve")
        }
      }

      "handle as XML" in withServer {
        case ("GET", "/") =>
          Action {
            Ok("""<?xml version="1.0" encoding="utf-8"?>
                 |<wrapper><message status="OK">Hello</message></wrapper>
              """.stripMargin).as("text/xml")
          }
        case other => Action { NotFound }
      } { ws =>
        // #scalaws-process-xml
        val futureResult: Future[scala.xml.NodeSeq] = ws.url(url).get().map { response =>
          response.xml \ "message"
        }
        // #scalaws-process-xml
        await(futureResult).text must_== "Hello"
      }

      "handle as stream" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeSource))
        case other        => Action { NotFound }
      } { ws =>
        //#stream-count-bytes
        // Make the request
        val futureResponse: Future[WSResponse] =
          ws.url(url).withMethod("GET").stream()

        val bytesReturned: Future[Long] = futureResponse.flatMap { res =>
          // Count the number of bytes returned
          res.bodyAsSource.runWith(Sink.fold[Long, ByteString](0L) { (total, bytes) =>
            total + bytes.length
          })
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000L
      }

      "stream to a file" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeSource))
        case other        => Action { NotFound }
      } { ws =>
        val file = File.createTempFile("stream-to-file-", ".txt")
        try {
          //#stream-to-file
          // Make the request
          val futureResponse: Future[WSResponse] =
            ws.url(url).withMethod("GET").stream()

          val downloadedFile: Future[File] = futureResponse.flatMap { res =>
            val outputStream = java.nio.file.Files.newOutputStream(file.toPath)

            // The sink that writes to the output stream
            val sink = Sink.foreach[ByteString] { bytes =>
              outputStream.write(bytes.toArray)
            }

            // materialize and run the stream
            res.bodyAsSource
              .runWith(sink)
              .andThen {
                case result =>
                  // Close the output stream whether there was an error or not
                  outputStream.close()
                  // Get the result or rethrow the error
                  result.get
              }
              .map(_ => file)
          }
          //#stream-to-file
          await(downloadedFile) must_== file

        } finally {
          file.delete()
        }
      }

      "stream to a result" in withServer {
        case ("GET", "/") => Action(Ok.chunked(largeSource))
        case other        => Action { NotFound }
      } { ws =>
        //#stream-to-result
        def downloadFile = Action.async {

          // Make the request
          ws.url(url).withMethod("GET").stream().map { response =>
            // Check that the response was successful
            if (response.status == 200) {

              // Get the content type
              val contentType = response.headers
                .get("Content-Type")
                .flatMap(_.headOption)
                .getOrElse("application/octet-stream")

              // If there's a content length, send that, otherwise return the body chunked
              response.headers.get("Content-Length") match {
                case Some(Seq(length)) =>
                  Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType)))
                case _ =>
                  Ok.chunked(response.bodyAsSource).as(contentType)
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
            .flatMap(_.body.dataStream.runFold(0L)((t, b) => t + b.length))
        ) must_== 10000L
        file.delete()
      }

      "stream when request is a PUT" in withServer {
        case ("PUT", "/") => Action(Ok.chunked(largeSource))
        case other        => Action { NotFound }
      } { ws =>
        //#stream-put
        val futureResponse: Future[WSResponse] =
          ws.url(url).withMethod("PUT").withBody("some body").stream()
        //#stream-put

        val bytesReturned: Future[Long] = futureResponse.flatMap { res =>
          res.bodyAsSource.runWith(Sink.fold[Long, ByteString](0L) { (total, bytes) =>
            total + bytes.length
          })
        }
        //#stream-count-bytes
        await(bytesReturned) must_== 10000L
      }

      "stream request body" in withServer {
        case ("PUT", "/") => Action(Ok(""))
        case other        => Action { NotFound }
      } { ws =>
        def largeImageFromDB: Source[ByteString, _] = largeSource
        //#scalaws-stream-request
        val wsResponse: Future[WSResponse] = ws
          .url(url)
          .withBody(largeImageFromDB)
          .execute("PUT")
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
      case other =>
        Action {
          NotFound
        }
    } { ws =>
      val urlOne       = s"http://localhost:$testServerPort/one"
      val exceptionUrl = s"http://localhost:$testServerPort/fallback"
      // #scalaws-forcomprehension
      val futureResponse: Future[WSResponse] = for {
        responseOne   <- ws.url(urlOne).get()
        responseTwo   <- ws.url(responseOne.body).get()
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

    "allow timeout across futures" in new WithServer() with Injecting {
      val url2             = url
      implicit val futures = inject[Futures]
      val ws               = inject[WSClient]
      //#ws-futures-timeout
      // Adds withTimeout as type enrichment on Future[WSResponse]
      import play.api.libs.concurrent.Futures._

      val result: Future[Result] =
        ws.url(url)
          .get()
          .withTimeout(1 second)
          .flatMap { response =>
            // val url2 = response.json \ "url"
            ws.url(url2).get().map { response2 =>
              Ok(response.body)
            }
          }
          .recover {
            case e: scala.concurrent.TimeoutException =>
              GatewayTimeout
          }
      //#ws-futures-timeout
      status(result) must_== OK
    }

    "allow simple programmatic configuration" in new WithApplication() {
      //#simple-ws-custom-client
      import play.api.libs.ws.ahc._

      // usually injected through @Inject()(implicit mat: Materializer)
      implicit val mat: akka.stream.Materializer = app.materializer
      val wsClient                               = AhcWSClient()
      //#simple-ws-custom-client

      wsClient.close()

      ok
    }

    "allow programmatic configuration" in new WithApplication() {

      //#ws-custom-client
      import com.typesafe.config.ConfigFactory
      import play.api._
      import play.api.libs.ws._
      import play.api.libs.ws.ahc._

      val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString("""
                                                                                               |ws.followRedirects = true
        """.stripMargin))

      // If running in Play, environment should be injected
      val environment        = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)
      val wsConfig           = AhcWSClientConfigFactory.forConfig(configuration.underlying, environment.classLoader)
      val wsClient: WSClient = AhcWSClient(wsConfig)
      //#ws-custom-client

      //#close-client
      wsClient.close()
      //#close-client

      ok
    }

    "grant access to the underlying client" in withSimpleServer { ws =>
      //#underlying
      import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

      val client: AsyncHttpClient = ws.underlying
      //#underlying

      ok
    }

    "use logging" in withSimpleServer { ws =>
      // #curl-logger-filter
      ws.url(s"http://localhost:$testServerPort")
        .withRequestFilter(AhcCurlRequestLogger())
        .put(Map("key" -> Seq("value")))
      // #curl-logger-filter

      ok
    }

  }

  // #ws-custom-body-readable
  trait URLBodyReadables {
    implicit val urlBodyReadable = BodyReadable[java.net.URL] { response =>
      import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
      val ahcResponse = response.underlying[AHCResponse]
      val s           = ahcResponse.getResponseBody
      java.net.URI.create(s).toURL
    }
  }
  // #ws-custom-body-readable

  // #ws-custom-body-writable
  trait URLBodyWritables {
    implicit val urlBodyWritable = BodyWritable[java.net.URL]({ url =>
      val s          = url.toURI.toString
      val byteString = ByteString.fromString(s)
      InMemoryBody(byteString)
    }, "text/plain")
  }
  // #ws-custom-body-writable

}
