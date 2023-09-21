/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs

import scala.annotation.meta.field
import scala.jdk.CollectionConverters._

import play.api.test._
import play.data.validation.Constraints.Required

class JavaFormSpec extends PlaySpecification {
  "A Java form" should {
    "throw a meaningful exception when get is called on an invalid form" in new WithApplication() {
      override def running() = {
        val formFactory = app.injector.instanceOf[play.data.FormFactory]
        val lang        = play.api.i18n.Lang.defaultLang.asJava
        val attrs       = play.libs.typedmap.TypedMap.empty()
        val myForm      = formFactory.form(classOf[FooForm]).bind(lang, attrs, Map("id" -> "1234567891").asJava)
        myForm.hasErrors must beEqualTo(true)
        myForm.get must throwAn[IllegalStateException].like {
          case e => e.getMessage must contain("fooName")
        }
      }
    }
  }
}

class FooForm {
  var id: Long = _

  @(Required @field)
  var fooName: String = _
}
