/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalatest.tests.specs2

import org.specs2.mutable._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class FunctionalTemplateSpec extends Specification {
  // #scalatest-functionaltemplatespec
  "render index template" in new WithApplication {
    override def running() = {
      val html = views.html.index("Coco")

      contentAsString(html) must contain("Hello Coco")
    }
  }
  // #scalatest-functionaltemplatespec
}
