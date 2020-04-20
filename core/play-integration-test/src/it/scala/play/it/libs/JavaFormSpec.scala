/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs

import play.api.test._
import play.data.validation.Constraints.Required

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.collection.JavaConverters._

class JavaFormSpec extends PlaySpecification {
  "A Java form" should {
    "throw a meaningful exception when get is called on an invalid form" in new WithApplication() {
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

class FooForm {
  @BeanProperty
  var id: Long = _

  @(Required @field)
  @BeanProperty
  var fooName: String = _
}
