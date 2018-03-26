/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import play.api.http.SessionConfiguration;
import play.api.libs.crypto.CSRFTokenSigner;
import play.core.j.JavaRequestComponents;
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
    private final JavaRequestComponents components;

    @Inject
    public AddCSRFTokenAction(CSRFConfig config, SessionConfiguration sessionConfiguration, CSRF.TokenProvider tokenProvider, CSRFTokenSigner tokenSigner, JavaRequestComponents components) {
        this.config = config;
        this.sessionConfiguration = sessionConfiguration;
        this.tokenProvider = tokenProvider;
        this.tokenSigner = tokenSigner;
        this.components = components;
    }

    private final CSRF.Token$ Token = CSRF.Token$.MODULE$;

    private static final String CSRF_TOKEN = "CSRF_TOKEN";
    private static final String CSRF_TOKEN_NAME = "CSRF_TOKEN_NAME";

    @Override
    public CompletionStage<Result> call(Http.Request req) {

        CSRFActionHelper helper =
            new CSRFActionHelper(sessionConfiguration, config, tokenSigner, tokenProvider);

        play.api.mvc.Request<RequestBody> request =
                helper.tagRequestFromHeader(req.asScala());

        if (helper.getTokenToValidate(request).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            CSRF.Token newToken = helper.generateToken();

            // Place this token into the context
            req.args().put(CSRF_TOKEN, newToken.value());
            req.args().put(CSRF_TOKEN_NAME, newToken.name());

            // Create a new Scala RequestHeader with the token
            request = helper.tagRequest(request, newToken);

            // Also add it to the response
            if (config.cookieName().isDefined()) {
                scala.Option<String> domain = sessionConfiguration.domain();
                Http.Cookie cookie = new Http.Cookie(
                    config.cookieName().get(), newToken.value(), null, sessionConfiguration.path(),
                    domain.isDefined() ? domain.get() : null, config.secureCookie(), config.httpOnlyCookie(), null);
                req.response().setCookie(cookie);
            } else {
                req.session().put(newToken.name(), newToken.value());
            }
        }

        final Request newRequest = new RequestImpl(request, components);

        Http.Context.current.set(newRequest);
        return delegate.call(newRequest);
    }
}
