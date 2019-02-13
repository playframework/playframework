/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import akka.stream.Materializer
import javax.inject._
import play.api.Configuration
import play.api.inject._

/**
 * Provider for Content Security Policy configuration.
 */
@Singleton
class CSPConfigProvider @Inject() (configuration: Configuration) extends Provider[CSPConfig] {
  lazy val get: CSPConfig = CSPConfig.fromConfiguration(configuration)
}

/**
 * The content security policy module.
 */
class CSPModule extends SimpleModule(
  bind[CSPConfig].toProvider[CSPConfigProvider],
  bind[CSPProcessor].to[DefaultCSPProcessor],

  bind[CSPResultProcessor].to[DefaultCSPResultProcessor],
  bind[CSPActionBuilder].to[DefaultCSPActionBuilder],
  bind[CSPFilter].toSelf,

  bind[CSPReportBodyParser].to[DefaultCSPReportBodyParser],
  bind[CSPReportActionBuilder].to[DefaultCSPReportActionBuilder]
)

/**
 * The content security policy components, for compile time dependency injection.
 */
trait CSPComponents extends play.api.BuiltInComponents {
  implicit def materializer: Materializer

  def configuration: Configuration

  lazy val cspConfig: CSPConfig = CSPConfig.fromConfiguration(configuration)
  lazy val cspProcessor: CSPProcessor = CSPProcessor(cspConfig)

  lazy val cspResultProcessor: CSPResultProcessor = CSPResultProcessor(cspProcessor)
  lazy val cspFilter: CSPFilter = CSPFilter(cspResultProcessor)
  lazy val cspActionBuilder: CSPActionBuilder = CSPActionBuilder(cspResultProcessor, playBodyParsers)

  lazy val cspReportBodyParser: CSPReportBodyParser = new DefaultCSPReportBodyParser(playBodyParsers)
  lazy val cspReportAction: CSPReportActionBuilder = new DefaultCSPReportActionBuilder(cspReportBodyParser)
}