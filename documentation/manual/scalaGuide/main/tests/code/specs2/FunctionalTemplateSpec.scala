<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/FunctionalTemplateSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalatest.tests
=======
package scalatest.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/FunctionalTemplateSpec.scala

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
