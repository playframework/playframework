/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Inject;

import play.api.http.SessionConfiguration;
import play.api.libs.crypto.CSRFTokenSigner;
import play.api.mvc.RequestHeader;
import play.api.mvc.Session;
import play.inject.Injector;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;

public class RequireCSRFCheckAction extends Action<RequireCSRFCheck> {

    private final CSRFConfig config;
    private final SessionConfiguration sessionConfiguration;
    private final CSRF.TokenProvider tokenProvider;
    private final CSRFTokenSigner tokenSigner;
    private Function<RequireCSRFCheck, CSRFErrorHandler> configurator;

    @Inject
    public RequireCSRFCheckAction(CSRFConfig config, SessionConfiguration sessionConfiguration, CSRF.TokenProvider tokenProvider, CSRFTokenSigner csrfTokenSigner, Injector injector) {
        this(config, sessionConfiguration, tokenProvider, csrfTokenSigner, configAnnotation -> injector.instanceOf(configAnnotation.error()));
    }

    public RequireCSRFCheckAction(CSRFConfig config, SessionConfiguration sessionConfiguration, CSRF.TokenProvider tokenProvider, CSRFTokenSigner csrfTokenSigner, CSRFErrorHandler errorHandler) {
        this(config, sessionConfiguration, tokenProvider, csrfTokenSigner, configAnnotation -> errorHandler);
    }

    public RequireCSRFCheckAction(CSRFConfig config, SessionConfiguration sessionConfiguration, CSRF.TokenProvider tokenProvider, CSRFTokenSigner csrfTokenSigner, Function<RequireCSRFCheck, CSRFErrorHandler> configurator) {
        this.config = config;
        this.sessionConfiguration = sessionConfiguration;
        this.tokenProvider = tokenProvider;
        this.tokenSigner = csrfTokenSigner;
        this.configurator = configurator;
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {

        CSRFActionHelper csrfActionHelper =
            new CSRFActionHelper(sessionConfiguration, config, tokenSigner, tokenProvider);

        RequestHeader request = csrfActionHelper.tagRequestFromHeader(ctx._requestHeader());
        // Check for bypass
        if (!csrfActionHelper.requiresCsrfCheck(request)) {
            return delegate.call(ctx);
        } else {
            // Get token from cookie/session
            Option<String> headerToken = csrfActionHelper.getTokenToValidate(request);
            if (headerToken.isDefined()) {
                String tokenToCheck = null;

                // Get token from query string
                Option<String> queryStringToken = csrfActionHelper.getHeaderToken(request);
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
                Option<String> domain = sessionConfiguration.domain();
                ctx.response().discardCookie(config.cookieName().get(), sessionConfiguration.path(),
                        domain.isDefined() ? domain.get() : null, config.secureCookie());
            } else {
                ctx.session().remove(config.tokenName());
            }
        }

        CSRFErrorHandler handler = configurator.apply(this.configuration);
        return handler.handle(request.asJava(), msg);
    }
}
