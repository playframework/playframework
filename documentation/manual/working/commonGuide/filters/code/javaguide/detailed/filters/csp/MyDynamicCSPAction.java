/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import play.filters.csp.*;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import scala.collection.JavaConverters;

// #java-csp-dynamic-action
public class MyDynamicCSPAction extends AbstractCSPAction {

    private final AssetCache assetCache;
    private final CSPConfig cspConfig;

    @Inject
    public MyDynamicCSPAction(CSPConfig cspConfig, AssetCache assetCache) {
        this.assetCache = assetCache;
        this.cspConfig = cspConfig;
    }

    private CSPConfig cspConfig() {
        return cspConfig.withDirectives(generateDirectives());
    }

    private List<CSPDirective> generateDirectives() {
        // import scala.collection.JavaConverters;
        List<CSPDirective> baseDirectives = JavaConverters.seqAsJavaList(cspConfig.directives());
        return baseDirectives.stream().map(directive -> {
            if ("script-src".equals(directive.name())) {
                String scriptSrc = directive.value();
                String newScriptSrc = scriptSrc + " " + String.join(" ", assetCache.cspHashes());
                return new CSPDirective("script-src", newScriptSrc);
            } else {
                return directive;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public CSPProcessor processor() {
        return new DefaultCSPProcessor(cspConfig());
    }
}
// #java-csp-dynamic-action
