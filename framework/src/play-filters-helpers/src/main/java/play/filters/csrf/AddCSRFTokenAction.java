/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import play.api.http.SessionConfiguration;
import play.api.libs.crypto.CSRFTokenSigner;
import play.api.mvc.Session;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import play.mvc.Http.RequestImpl;
import play.mvc.Result;

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

    private static final String CSRF_TOKEN = "CSRF_TOKEN";
    private static final String CSRF_TOKEN_NAME = "CSRF_TOKEN_NAME";

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {

        CSRFActionHelper csrfActionHelper = new CSRFActionHelper(sessionConfiguration, config, tokenSigner);

        play.api.mvc.Request<RequestBody> request =
                csrfActionHelper.tagRequestFromHeader(ctx.request()._underlyingRequest());

        if (csrfActionHelper.getTokenToValidate(request).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            String newToken = tokenProvider.generateToken();

            // Place this token into the context
            ctx.args.put(CSRF_TOKEN, newToken);
            ctx.args.put(CSRF_TOKEN_NAME, config.tokenName());

            // Create a new Scala RequestHeader with the token
            request = csrfActionHelper.tagRequest(request, new CSRF.Token(config.tokenName(), newToken));

            // Also add it to the response
            if (config.cookieName().isDefined()) {
                scala.Option<String> domain = Session.domain();
                Http.Cookie cookie = new Http.Cookie(config.cookieName().get(), newToken, null, Session.path(),
                        domain.isDefined() ? domain.get() : null, config.secureCookie(), config.httpOnlyCookie());
                ctx.response().setCookie(cookie);
            } else {
                ctx.session().put(config.tokenName(), newToken);
            }
        }

        final play.api.mvc.Request<RequestBody> newRequest = request;
        // Methods returning requests should return the tagged request
        Http.Context newCtx = new Http.WrappedContext(ctx) {
            @Override
            public Request request() {
                return new RequestImpl(newRequest);
            }

            @Override
            public play.api.mvc.RequestHeader _requestHeader() {
                return newRequest;
            }
        };

        Http.Context.current.set(newCtx);
        return delegate.call(newCtx);
    }
}
