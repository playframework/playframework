/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data

import scala.jdk.CollectionConverters._

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.i18n._
import play.data.format.Formatters
import play.data.validation.Constraints.MaxLength
import play.data.validation.Constraints.Required
import play.libs.typedmap.TypedMap

class PartialValidationSpec extends Specification {
  val messagesApi = new DefaultMessagesApi()

  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
  val formFactory =
    new FormFactory(jMessagesApi, new Formatters(jMessagesApi), FormSpec.validatorFactory(), ConfigFactory.load())

  "partial validation" should {
    "not fail when fields not in the same group fail validation" in {
      val form =
        formFactory
          .form(classOf[SomeForm], classOf[Partial])
          .bind(Lang.defaultLang.asJava, TypedMap.empty(), Map("prop2" -> "Hello", "prop3" -> "abc").asJava)
      form.errors().asScala must beEmpty
    }

    "fail when a field in the group fails validation" in {
      val form = formFactory
        .form(classOf[SomeForm], classOf[Partial])
        .bind(Lang.defaultLang.asJava, TypedMap.empty(), Map("prop3" -> "abc").asJava)
      form.hasErrors must_== true
    }

    "support multiple validations for the same group" in {
      val form1 = formFactory
        .form(classOf[SomeForm])
        .bind(Lang.defaultLang.asJava, TypedMap.empty(), Map("prop2" -> "Hello").asJava)
      form1.hasErrors must_== true
      val form2 = formFactory
        .form(classOf[SomeForm])
        .bind(Lang.defaultLang.asJava, TypedMap.empty(), Map("prop2" -> "Hello", "prop3" -> "abcd").asJava)
      form2.hasErrors must_== true
    }
  }
}

trait Partial

class SomeForm {
  @Required
  var prop1: String           = _
  def getProp1()              = prop1
  def setProp1(prop1: String) = this.prop1 = prop1

  @Required(groups = Array(classOf[Partial]))
  var prop2: String           = _
  def getProp2()              = prop2
  def setProp2(prop2: String) = this.prop2 = prop2

  @Required(groups = Array(classOf[Partial]))
  @MaxLength(value = 3, groups = Array(classOf[Partial]))
  var prop3: String           = _
  def getProp3()              = prop3
  def setProp3(prop3: String) = this.prop3 = prop3
}
