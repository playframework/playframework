/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data

import java.util.Locale

import scala.jdk.CollectionConverters._

import com.typesafe.config.ConfigFactory
import jakarta.validation.{ Configuration => vConfiguration }
import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext
import org.specs2.mutable.Specification
import play.api.i18n._
import play.data.format.Formatters
import play.libs.typedmap.TypedMap

class CustomValidatorFactoryLocaleSpec extends Specification {
  val jLangs       = new DefaultLangs(Seq(Lang("de"), Lang("en"))).asJava
  val jMessagesApi = new play.i18n.MessagesApi(new DefaultMessagesApi(langs = jLangs.asScala()))

  // An application-provided factory whose message interpolator selects the locale on its own
  val validationConfig: vConfiguration[?] = Validation
    .byDefaultProvider()
    .configure()
    .messageInterpolator(
      new ParameterMessageInterpolator(
        Set(Locale.FRENCH).asJava,
        Locale.FRENCH,
        new LocaleResolver {
          override def resolve(context: LocaleResolverContext): Locale = Locale.FRENCH
        },
        false
      )
    )
  val validatorFactory = validationConfig.buildValidatorFactory()

  val formFactory =
    new FormFactory(jMessagesApi, jLangs, new Formatters(jMessagesApi), validatorFactory, ConfigFactory.load())

  "a custom validator factory" should {
    "keep its own locale selection when a form is bound with a language" in {
      val form = formFactory
        .form(classOf[JavaI18NValidatorForm])
        .bind(Lang("en").asJava, TypedMap.empty(), Map("note" -> "foo").asJava)
      form.errors("note").get(0).message() must beEqualTo("la taille doit être comprise entre 10 et 100")
    }
  }

  step(validatorFactory.close())
}
