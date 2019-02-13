/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp;

import play.api.mvc.request.RequestAttrKey;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;

import java.util.concurrent.CompletionStage;

import static scala.compat.java8.OptionConverters.*;

/**
 * Processes a request and adds content security policy header.
 */
public abstract class AbstractCSPAction extends Action<CSP> {

    public abstract CSPProcessor processor();

    @Override
    public CompletionStage<Result> call(Http.Request request) {
        Option<CSPResult> maybeResult = processor().process(request.asScala());
        if (maybeResult.isEmpty()) {
            return delegate.call(request);
        }
        final CSPResult cspResult = maybeResult.get();

        Http.Request newRequest = toJava(cspResult.nonce())
                .map(n -> request.addAttr(RequestAttrKey.CSPNonce().asJava(), n))
                .orElseGet(() -> request);

        return delegate.call(newRequest).thenApply((Result result) -> {
            Result r = result;
            if (cspResult.nonceHeader()) {
                r = r.withHeader(Http.HeaderNames.X_CONTENT_SECURITY_POLICY_NONCE_HEADER, cspResult.nonce().get());
            }
            return r.withHeader(Http.HeaderNames.CONTENT_SECURITY_POLICY, cspResult.directives());
        });
    }
}
