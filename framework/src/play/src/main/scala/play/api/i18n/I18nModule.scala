/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import play.api.http.HttpConfiguration
import play.api.{ Configuration, Environment }
import play.api.inject.Module

class I18nModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Langs].toProvider[DefaultLangsProvider],
      bind[MessagesApi].toProvider[DefaultMessagesApiProvider],
      bind[play.i18n.MessagesApi].toSelf,
      bind[play.i18n.Langs].toSelf
    )
  }
}

/**
 * Injection helper for i18n components
 */
trait I18nComponents {

  def environment: Environment
  def configuration: Configuration
  def httpConfiguration: HttpConfiguration

  lazy val langs: Langs = new DefaultLangsProvider(configuration).get
  lazy val messagesApi: MessagesApi = new DefaultMessagesApiProvider(environment, configuration, langs, httpConfiguration).get

}
