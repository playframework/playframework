/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import play.filters.csp.*;
import play.mvc.Controller;
import play.mvc.Result;

// #csp-action-controller
public class CSPActionController extends Controller {
  @CSP
  public Result index() {
    return ok("result with CSP header");
  }
}
// #csp-action-controller
