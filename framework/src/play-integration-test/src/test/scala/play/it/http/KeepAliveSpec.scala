package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer

object KeepAliveSpec extends PlaySpecification {

  // sequential execution needed, otherwise one of the specs described
  // below will fail if running in parallel
  sequential

  "Play" should {

    def withServer[T](action: EssentialAction, keepAliveDisabled: Boolean)(block: Port => T) =
      try {
        val port = testServerPort

        if (keepAliveDisabled)
          System.setProperty("http.keepAliveDisabled", "yes")
        else
          System.setProperty("http.keepAliveDisabled", "no")

        running(TestServer(port, FakeApplication(
          withRoutes = {
            case _ => action
          }
        ))) {
          block(port)
        }
      }
      finally {
        System.clearProperty("http.keepAliveDisabled")
      }

    "Keep-Alive connections by default" in
      withServer(Action(Results.Ok), keepAliveDisabled = false) { port =>
        val client = new BasicHttpClient(testServerPort)
        try {
          val responses =
            client.sendRequest(BasicRequest("GET", "/", "HTTP/1.1", Map.empty, ""), "0", true, None) ++
            client.sendRequest(BasicRequest("GET", "/", "HTTP/1.1", Map.empty, ""), "1", true, None)

          responses.length must_== 2
          responses(0).status must_== 200
          responses(1).status must_== 200
        }
        finally {
          client.close()
        }
      }

    "honour http.keepAliveDisabled=yes and close all connections" in
      withServer(Action(Results.Ok), keepAliveDisabled = true) { port =>
        val client = new BasicHttpClient(testServerPort)
        try {
          val responses =
            client.sendRequest(BasicRequest("GET", "/", "HTTP/1.1", Map.empty, ""), "0", true, None)

          responses.length must_== 1
          responses.head.status must_== 200

          // making second request using same connection, should not receive response
          client.sendRequest(BasicRequest("GET", "/", "HTTP/1.1", Map.empty, ""), "1", false, None)
          client.reader.readLine() must_== null
        }
        finally {
          client.close()
        }
      }
  }
}
