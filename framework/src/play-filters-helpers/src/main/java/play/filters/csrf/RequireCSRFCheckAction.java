/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf;

import play.api.libs.Crypto;
import play.api.mvc.RequestHeader;
import play.api.mvc.Session;
import play.inject.Injector;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class RequireCSRFCheckAction extends Action<RequireCSRFCheck> {

    private final CSRFConfig config;
    private final CSRF.TokenProvider tokenProvider;
    private final Crypto crypto;
    private final Injector injector;

    @Inject
    public RequireCSRFCheckAction(CSRFConfig config, CSRF.TokenProvider tokenProvider, Crypto crypto, Injector injector) {
        this.config = config;
        this.tokenProvider = tokenProvider;
        this.crypto = crypto;
        this.injector = injector;
    }

    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        RequestHeader request = CSRFAction.tagRequestFromHeader(ctx._requestHeader(), config, crypto);
        // Check for bypass
        if (!CSRFAction.requiresCsrfCheck(request, config)) {
            return delegate.call(ctx);
        } else {
            // Get token from cookie/session
            Option<String> headerToken = CSRFAction.getTokenToValidate(request, config, crypto);
            if (headerToken.isDefined()) {
                String tokenToCheck = null;

                // Get token from query string
                Option<String> queryStringToken = CSRFAction.getHeaderToken(request, config);
                if (queryStringToken.isDefined()) {
                    tokenToCheck = queryStringToken.get();
                } else {

                    // Get token from body
                    if (ctx.request().body().asFormUrlEncoded() != null) {
                        String[] values = ctx.request().body().asFormUrlEncoded().get(config.tokenName());
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    } else if (ctx.request().body().asMultipartFormData() != null) {
                        Map<String, String[]> form = ctx.request().body().asMultipartFormData().asFormUrlEncoded();
                        String[] values = form.get(config.tokenName());
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    }
                }

                if (tokenToCheck != null) {
                    if (tokenProvider.compareTokens(tokenToCheck, headerToken.get())) {
                        return delegate.call(ctx);
                    } else {
                        return handleTokenError(ctx, request, "CSRF tokens don't match");
                    }
                } else {
                    return handleTokenError(ctx, request, "CSRF token not found in body or query string");
                }
            } else {
                return handleTokenError(ctx, request, "CSRF token not found in session");
            }
        }
    }

    private CompletionStage<Result> handleTokenError(Http.Context ctx, RequestHeader request, String msg) {

        if (CSRF.getToken(request).isEmpty()) {
            if (config.cookieName().isDefined()) {
                Option<String> domain = Session.domain();
                ctx.response().discardCookie(config.cookieName().get(), Session.path(),
                        domain.isDefined() ? domain.get() : null, config.secureCookie());
            } else {
                ctx.session().remove(config.tokenName());
            }
        }

        CSRFErrorHandler handler = injector.instanceOf(configuration.error());
        return handler.handle(new Http.RequestImpl(request), msg);
    }
}
