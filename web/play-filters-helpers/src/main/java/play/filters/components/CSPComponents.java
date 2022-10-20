/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.ConfigurationComponents;
import play.filters.csp.*;

/** The Java CSP components. */
public interface CSPComponents extends ConfigurationComponents {

  default CSPConfig cspConfig() {
    return CSPConfig$.MODULE$.fromConfiguration(configuration());
  }

  default CSPProcessor cspProcessor() {
    return new DefaultCSPProcessor(cspConfig());
  }

  default CSPResultProcessor cspResultProcessor() {
    return new DefaultCSPResultProcessor(cspProcessor());
  }

  default CSPFilter cspFilter() {
    return new CSPFilter(cspResultProcessor());
  }

  default CSPAction cspAction() {
    return new CSPAction(cspProcessor());
  }
}
