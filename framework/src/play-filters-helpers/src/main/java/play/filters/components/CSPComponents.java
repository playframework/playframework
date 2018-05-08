/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.ConfigurationComponents;

import play.filters.csp.*;

/**
 * The Java CSP components.
 */
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
