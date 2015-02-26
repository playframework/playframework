/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import play.api.test._
import play.core.j.JavaHelpers
import play.data.Form
import play.data.validation.Constraints.Required
import play.mvc.Http.Context
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.collection.JavaConverters._

object JavaFormSpec extends PlaySpecification {

  "A Java form" should {

    "throw a meaningful exception when get is called on an invalid form" in new WithApplication() {
      val javaContext = JavaHelpers.createJavaContext(FakeRequest())
      try {
        Context.current.set(javaContext)
        val myForm = Form.form(classOf[FooForm]).bind(Map("id" -> "1234567891").asJava)
        myForm.hasErrors must beEqualTo(true)
        myForm.get must throwAn[IllegalStateException].like {
          case e => e.getMessage must contain("fooName")
        }
      } finally {
        Context.current.remove()
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
