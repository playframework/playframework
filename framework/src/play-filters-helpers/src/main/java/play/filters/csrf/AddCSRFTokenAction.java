/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf;

import java.util.concurrent.CompletionStage;

import play.api.libs.crypto.CSRFTokenSigner;
import play.api.mvc.RequestHeader;
import play.api.mvc.Session;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.RequestImpl;
import play.mvc.Result;
import scala.Option;

import javax.inject.Inject;

public class AddCSRFTokenAction extends Action<AddCSRFToken> {

    private final CSRFConfig config;
    private final CSRF.TokenProvider tokenProvider;
    private final CSRFTokenSigner tokenSigner;

    @Inject
    public AddCSRFTokenAction(CSRFConfig config, CSRF.TokenProvider tokenProvider, CSRFTokenSigner tokenSigner) {
        this.config = config;
        this.tokenProvider = tokenProvider;
        this.tokenSigner = tokenSigner;
    }

    private final CSRF.Token$ Token = CSRF.Token$.MODULE$;
    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        RequestHeader request = CSRFAction.tagRequestFromHeader(ctx._requestHeader(), config, tokenSigner);

        if (CSRFAction.getTokenToValidate(request, config, tokenSigner).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            String newToken = tokenProvider.generateToken();

            // Place this token into the context
            ctx.args.put(Token.RequestTag(), newToken);
            ctx.args.put(Token.NameRequestTag(), config.tokenName());

            // Create a new Scala RequestHeader with the token
            request = CSRFAction.tagRequest(request, new CSRF.Token(config.tokenName(), newToken));

            // Also add it to the response
            if (config.cookieName().isDefined()) {
                Option<String> domain = Session.domain();
                ctx.response().setCookie(config.cookieName().get(), newToken, null, Session.path(),
                        domain.isDefined() ? domain.get() : null, config.secureCookie(), config.httpOnlyCookie());
            } else {
                ctx.session().put(config.tokenName(), newToken);
            }
        }

        final RequestHeader newRequest = request;
        // Methods returning requests should return the tagged request
        Http.Context newCtx = new Http.WrappedContext(ctx) {
            @Override
            public Request request() {
                return new RequestImpl(newRequest);
            }

            @Override
            public RequestHeader _requestHeader() {
                return newRequest;
            }
        };

        Http.Context.current.set(newCtx);
        return delegate.call(newCtx);
    }
}
