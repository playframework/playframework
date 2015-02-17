/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.xml.scalaxmlrequests {

import play.api.mvc._
import play.api.test._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ScalaXmlRequestsSpec extends PlaySpecification with Controller {

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

      testAction(sayHello, FakeRequest().withXmlBody(<name>XF</name>))
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

      testAction(sayHello, FakeRequest().withXmlBody(<name>XF</name>).map(_.xml))
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

      testAction(sayHello, FakeRequest().withXmlBody(<name>XF</name>).map(_.xml))
    }



  }

  def testAction[T](action: Action[T], req: Request[T]) = {

    running(FakeApplication()) {
      val result = action(req)
      status(result) must_== OK
    }
  }
}

}
