/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import play.api.mvc._
import javax.inject._
import play.filters.csp.{CSPReportActionBuilder, ScalaCSPReport}

// #csp-report-controller
class CSPReportController @Inject()(cc: ControllerComponents,
                                    cspReportAction: CSPReportActionBuilder)
  extends AbstractController(cc) {

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  val report: Action[ScalaCSPReport] = cspReportAction { request =>
    val report = request.body
    logger.warn(s"CSP violation: violated-directive = ${report.violatedDirective}, " +
      s"blocked = ${report.blockedUri}, " +
      s"policy = ${report.originalPolicy}")
    Ok("{}").as(JSON)
  }
}
// #csp-report-controller