/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters.csp

import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}
import play.filters.csp.CSPActionBuilder

// #csp-action-controller
class CSPActionController @Inject()(cspAction: CSPActionBuilder,
                                    cc: ControllerComponents)
  extends AbstractController(cc) {
  def index = cspAction { implicit request =>
    Ok("result containing CSP")
  }
}
// #csp-action-controller