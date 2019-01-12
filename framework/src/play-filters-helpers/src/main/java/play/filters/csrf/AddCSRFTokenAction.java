/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import play.api.http.SessionConfiguration;
import play.api.libs.crypto.CSRFTokenSigner;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.RequestBody;
import play.mvc.Http.RequestImpl;
import play.mvc.Result;
import scala.compat.java8.OptionConverters;

public class AddCSRFTokenAction extends Action<AddCSRFToken> {

    private final CSRFConfig config;
    private final SessionConfiguration sessionConfiguration;
    private final CSRF.TokenProvider tokenProvider;
    private final CSRFTokenSigner tokenSigner;

    @Inject
    public AddCSRFTokenAction(CSRFConfig config, SessionConfiguration sessionConfiguration, CSRF.TokenProvider tokenProvider, CSRFTokenSigner tokenSigner) {
        this.config = config;
        this.sessionConfiguration = sessionConfiguration;
        this.tokenProvider = tokenProvider;
        this.tokenSigner = tokenSigner;
    }

    private final CSRF.Token$ Token = CSRF.Token$.MODULE$;

    @Override
    public CompletionStage<Result> call(Http.Request req) {

        CSRFActionHelper helper =
            new CSRFActionHelper(sessionConfiguration, config, tokenSigner, tokenProvider);

        play.api.mvc.Request<RequestBody> taggedRequest =
                helper.tagRequestFromHeader(req.asScala());

        if (helper.getTokenToValidate(taggedRequest).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            CSRF.Token newToken = helper.generateToken();

            // Create a new Scala RequestHeader with the token
            taggedRequest = helper.tagRequest(taggedRequest, newToken);

            // Also add it to the response
            return delegate.call(new RequestImpl(taggedRequest)).thenApply(result -> placeToken(req, result, newToken));
        }
        return delegate.call(new RequestImpl(taggedRequest));
    }

    /**
     * Places the CSRF token in the session or in a cookie (if a cookie name is configured)
     */
    private Result placeToken(Http.Request req, final Result result, CSRF.Token token) {
        if (config.cookieName().isDefined()) {
            scala.Option<String> domain = sessionConfiguration.domain();
            Http.Cookie cookie = new Http.Cookie(
                    config.cookieName().get(), token.value(), null, sessionConfiguration.path(),
                    domain.isDefined() ? domain.get() : null, config.secureCookie(), config.httpOnlyCookie(), OptionConverters.toJava(config.sameSiteCookie()).map(c -> c.asJava()).orElse(null));
            return result.withCookies(cookie);
        }
        return result.addingToSession(req, token.name(), token.value());
    }
}
