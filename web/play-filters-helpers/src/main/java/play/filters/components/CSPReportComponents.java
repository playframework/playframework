/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.BodyParserComponents;
import play.filters.csp.CSPReportActionBuilder;
import play.filters.csp.CSPReportBodyParser;
import play.filters.csp.DefaultCSPReportActionBuilder;
import play.filters.csp.DefaultCSPReportBodyParser;

/** Components for reporting CSP violations. */
public interface CSPReportComponents extends BodyParserComponents {

  default CSPReportBodyParser cspReportBodyParser() {
    return new DefaultCSPReportBodyParser(scalaBodyParsers(), executionContext());
  }

  default CSPReportActionBuilder cspReportAction() {
    return new DefaultCSPReportActionBuilder(cspReportBodyParser(), executionContext());
  }
}
