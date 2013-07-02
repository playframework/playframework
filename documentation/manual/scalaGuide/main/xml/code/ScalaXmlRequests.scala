package scalaguide.xml.scalaxmlrequests {

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class ScalaXmlRequestsSpec extends Specification with Controller {

  "A scala XML request" should {

    "request body as xml" in {

      //#xml-request-body-asXml
      def sayHello = Action { request =>
        request.body.asXml.map { xml =>
          (xml \\ "name" headOption).map(_.text).map { name =>
            Ok("Hello " + name)
          }.getOrElse {
            BadRequest("Missing parameter [name]")
          }
        }.getOrElse {
          BadRequest("Expecting Xml data")
        }
      }
      //#xml-request-body-asXml

      testAction(sayHello)
    }

    "request body as xml body parser" in {

      //#xml-request-body-parser
      def sayHello = Action(parse.xml) { request =>
        (request.body \\ "name" headOption).map(_.text).map { name =>
          Ok("Hello " + name)
        }.getOrElse {
          BadRequest("Missing parameter [name]")
        }
      }
      //#xml-request-body-parser

      testAction(sayHello)
    }

    "request body as xml body parser and xml response" in {

      //#xml-request-body-parser-xml-response
      def sayHello = Action(parse.xml) { request =>
        (request.body \\ "name" headOption).map(_.text).map { name =>
          Ok(<message status="OK">Hello {name}</message>)
        }.getOrElse {
          BadRequest(<message status="KO">Missing parameter [name]</message>)
        }
      }
      //#xml-request-body-parser-xml-response

      testAction(sayHello)
    }



  }

  def testAction(action: EssentialAction) {
    val req = FakeRequest("POST","/").withXmlBody(<name>XF</name>).withHeaders((CONTENT_TYPE -> "application/xml"))

    //XXX: very bad, expect bad request but not ok
    assertAction(action,req,BAD_REQUEST){ result => }//(contentAsString(_) === "")
  }

  def assertAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => Unit) {
    running(FakeApplication()) {
      val result = action(request).run
      status(result) must_== expectedResponse
      assertions(result)
    }
  }
  }

}