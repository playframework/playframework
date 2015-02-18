/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data

import validation.Constraints.{ MaxLength, Required }
import beans.BeanProperty
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

class PartialValidationSpec extends Specification {
  "partial validation" should {
    "not fail when fields not in the same group fail validation" in {
      val form = Form.form(classOf[SomeForm], classOf[Partial]).bind(Map("prop2" -> "Hello", "prop3" -> "abc").asJava)
      form.errors().asScala must beEmpty
    }

    "fail when a field in the group fails validation" in {
      val form = Form.form(classOf[SomeForm], classOf[Partial]).bind(Map("prop3" -> "abc").asJava)
      form.hasErrors must_== true
    }

    "support multiple validations for the same group" in {
      val form1 = Form.form(classOf[SomeForm]).bind(Map("prop2" -> "Hello").asJava)
      form1.hasErrors must_== true
      val form2 = Form.form(classOf[SomeForm]).bind(Map("prop2" -> "Hello", "prop3" -> "abcd").asJava)
      form2.hasErrors must_== true
    }
  }
}

trait Partial

class SomeForm {

  @BeanProperty
  @Required
  var prop1: String = _

  @BeanProperty
  @Required(groups = Array(classOf[Partial]))
  var prop2: String = _

  @BeanProperty
  @Required(groups = Array(classOf[Partial]))
  @MaxLength(value = 3, groups = Array(classOf[Partial]))
  var prop3: String = _
}
