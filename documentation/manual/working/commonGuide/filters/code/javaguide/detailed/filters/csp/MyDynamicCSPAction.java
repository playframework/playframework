package javaguide.detailed.filters.csp;

import play.filters.csp.*;

import javax.inject.Inject;
import java.util.Optional;

import static scala.compat.java8.OptionConverters.toJava;

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

    private CSPDirectivesConfig generateDirectives() {
        CSPDirectivesConfig baseDirectives = cspConfig.directives();
        // import static scala.compat.java8.OptionConverters.toJava;
        String scriptSrc = toJava(baseDirectives.scriptSrc()).orElseGet(() -> "");
        String modifiedScriptSrc = scriptSrc + " " + String.join(" ", assetCache.cspHashes());
        return baseDirectives.withScriptSrc(Optional.of(modifiedScriptSrc));
    }

    @Override
    public CSPProcessor processor() {
        return new DefaultCSPProcessor(cspConfig());
    }
}
// #java-csp-dynamic-action
