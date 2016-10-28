/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.i18n

import play.api.{ Configuration, Environment }
import play.api.inject.Module

class I18nModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Langs].to[DefaultLangs],
      bind[MessagesApi].to[DefaultMessagesApi]
    )
  }
}

/**
 * Injection helper for i18n components
 */
trait I18nComponents {

  def environment: Environment
  def configuration: Configuration

  lazy val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, langs)
  lazy val langs: Langs = new DefaultLangs(configuration)

}
