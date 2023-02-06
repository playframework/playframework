/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import javax.inject._

import play.api.mvc._
import play.filters.csp.CSPReportActionBuilder
import play.filters.csp.ScalaCSPReport

// #csp-report-controller
class CSPReportController @Inject() (cc: ControllerComponents, cspReportAction: CSPReportActionBuilder)
    extends AbstractController(cc) {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  val report: Action[ScalaCSPReport] = cspReportAction { request =>
    val report = request.body
    logger.warn(
      s"CSP violation: violated-directive = ${report.violatedDirective}, " +
        s"blocked = ${report.blockedUri}, " +
        s"policy = ${report.originalPolicy}"
    )
    Ok("{}").as(JSON)
  }
}
// #csp-report-controller
