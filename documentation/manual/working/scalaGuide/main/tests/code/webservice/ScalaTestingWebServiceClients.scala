/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.webservice

package client {
//#client
import javax.inject.Inject
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class GitHubClient(ws: WSClient, baseUrl: String) {
  @Inject def this(ws: WSClient) = this(ws, "https://api.github.com")

  def repositories(): Future[Seq[String]] = {
    ws.url(baseUrl + "/repositories").get().map { response =>
      (response.json \\ "full_name").map(_.as[String])
    }
  }
}
//#client
}

package test {

import client._

//#full-test
import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

object GitHubClientSpec extends Specification with NoTimeConversions {

  "GitHubClient" should {
    "get all repositories" in {

      Server.withRouter() {
        case GET(p"/repositories") => Action {
          Results.Ok(Json.arr(Json.obj("full_name" -> "octocat/Hello-World")))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val result = Await.result(
            new GitHubClient(client, "").repositories(), 10.seconds)
          result must_== Seq("octocat/Hello-World")
        }
      }
    }
  }
}
//#full-test

}

import client._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

object ScalaTestingWebServiceClients extends Specification with NoTimeConversions {

  "webservice testing" should {
    "allow mocking a service" in {

      //#mock-service
      import play.api.libs.json._
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.Server

      Server.withRouter() {
        case GET(p"/repositories") => Action {
          Results.Ok(Json.arr(Json.obj("full_name" -> "octocat/Hello-World")))
        }
      } { implicit port =>
        //#mock-service
        ok
      }
    }

    "allow sending a resource" in {
      //#send-resource
      import play.api.mvc._
      import play.api.routing.sird._
      import play.api.test._
      import play.core.server.Server

      Server.withRouter() {
        case GET(p"/repositories") => Action {
          Results.Ok.sendResource("github/repositories.json")
        }
      } { implicit port =>
        //#send-resource
        WsTestClient.withClient { client =>
          Await.result(new GitHubClient(client, "").repositories(), 10.seconds) must_== Seq("octocat/Hello-World")
        }
      }
    }

    "allow being dry" in {
      //#with-github-client
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.Server
      import play.api.test._

      def withGitHubClient[T](block: GitHubClient => T): T = {
        Server.withRouter() {
          case GET(p"/repositories") => Action {
            Results.Ok.sendResource("github/repositories.json")
          }
        } { implicit port =>
          WsTestClient.withClient { client =>
            block(new GitHubClient(client, ""))
          }
        }
      }
      //#with-github-client

      //#with-github-test
      withGitHubClient { client =>
        val result = Await.result(client.repositories(), 10.seconds)
        result must_== Seq("octocat/Hello-World")
      }
      //#with-github-test
    }

  }
}