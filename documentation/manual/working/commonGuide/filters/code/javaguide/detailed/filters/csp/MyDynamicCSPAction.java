/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import play.filters.csp.*;
import scala.jdk.javaapi.CollectionConverters;

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
    List<CSPDirective> baseDirectives = CollectionConverters.asJava(cspConfig.directives());
    return baseDirectives.stream()
        .map(
            directive -> {
              if ("script-src".equals(directive.name())) {
                String scriptSrc = directive.value();
                String newScriptSrc = scriptSrc + " " + String.join(" ", assetCache.cspHashes());
                return new CSPDirective("script-src", newScriptSrc);
              } else {
                return directive;
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public CSPProcessor processor() {
    return new DefaultCSPProcessor(cspConfig());
  }
}
// #java-csp-dynamic-action
