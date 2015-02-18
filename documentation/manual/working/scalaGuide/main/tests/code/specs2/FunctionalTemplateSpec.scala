/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalatest.tests.specs2

import org.specs2.mutable._

import play.api.mvc._

import play.api.test._
import play.api.test.Helpers._

object FunctionalTemplateSpec extends Specification {

  // #scalatest-functionaltemplatespec
  "render index template" in new WithApplication {
    val html = views.html.index("Coco")

    contentAsString(html) must contain("Hello Coco")
  }
  // #scalatest-functionaltemplatespec

}
