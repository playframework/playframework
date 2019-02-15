/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Inject;

import play.api.http.SessionConfiguration;
import play.api.libs.crypto.CSRFTokenSigner;
import play.api.mvc.RequestHeader;
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
    public CompletionStage<Result> call(Http.Request req) {

        CSRFActionHelper csrfActionHelper =
            new CSRFActionHelper(sessionConfiguration, config, tokenSigner, tokenProvider);

        RequestHeader taggedRequest = csrfActionHelper.tagRequestFromHeader(req.asScala());
        // Check for bypass
        if (!csrfActionHelper.requiresCsrfCheck(taggedRequest)) {
            return delegate.call(req);
        } else {
            // Get token from cookie/session
            Option<String> headerToken = csrfActionHelper.getTokenToValidate(taggedRequest);
            if (headerToken.isDefined()) {
                String tokenToCheck = null;

                // Get token from query string
                Option<String> queryStringToken = csrfActionHelper.getHeaderToken(taggedRequest);
                if (queryStringToken.isDefined()) {
                    tokenToCheck = queryStringToken.get();
                } else {

                    // Get token from body
                    if (req.body().asFormUrlEncoded() != null) {
                        String[] values = req.body().asFormUrlEncoded().get(config.tokenName());
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    } else if (req.body().asMultipartFormData() != null) {
                        Map<String, String[]> form = req.body().asMultipartFormData().asFormUrlEncoded();
                        String[] values = form.get(config.tokenName());
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    }
                }

                if (tokenToCheck != null) {
                    if (tokenProvider.compareTokens(tokenToCheck, headerToken.get())) {
                        return delegate.call(req);
                    } else {
                        return handleTokenError(req, taggedRequest, "CSRF tokens don't match");
                    }
                } else {
                    return handleTokenError(req, taggedRequest, "CSRF token not found in body or query string");
                }
            } else {
                return handleTokenError(req, taggedRequest, "CSRF token not found in session");
            }
        }
    }

    private CompletionStage<Result> handleTokenError(Http.Request req, RequestHeader taggedRequest, String msg) {
        CSRFErrorHandler handler = configurator.apply(this.configuration);
        return handler.handle(taggedRequest.asJava(), msg).thenApply(result -> {
            if (CSRF.getToken(taggedRequest).isEmpty()) {
                if (config.cookieName().isDefined()) {
                    Option<String> domain = sessionConfiguration.domain();
                    return result.discardingCookie(config.cookieName().get(), sessionConfiguration.path(),
                            domain.isDefined() ? domain.get() : null, config.secureCookie());
                }
                return result.removingFromSession(req, config.tokenName());
            }
            return result;
        });
    }
}
