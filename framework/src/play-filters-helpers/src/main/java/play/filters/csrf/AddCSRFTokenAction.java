/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf;

import play.api.mvc.RequestHeader;
import play.api.mvc.Session;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;
import scala.Tuple2;

import javax.inject.Inject;

public class AddCSRFTokenAction extends Action<AddCSRFToken> {

    private final CSRFConfig config;
    private final CSRF.TokenProvider tokenProvider;

    @Inject
    public AddCSRFTokenAction(CSRFConfig config, CSRF.TokenProvider tokenProvider) {
        this.config = config;
        this.tokenProvider = tokenProvider;
    }

    private final String requestTag = CSRF.Token$.MODULE$.RequestTag();
    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;

    @Override
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        RequestHeader request = ctx._requestHeader();

        if (CSRFAction.getTokenFromHeader(request, config).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            String newToken = tokenProvider.generateToken();

            // Place this token into the context
            ctx.args.put(requestTag, newToken);

            // Create a new Scala RequestHeader with the token
            final RequestHeader newRequest = request.copy(request.id(),
                    request.tags().$plus(new Tuple2<String, String>(requestTag, newToken)),
                    request.uri(), request.path(), request.method(), request.version(), request.queryString(),
                    request.headers(), request.remoteAddress(), request.secure());

            // Create a new context that will have the new RequestHeader.  This ensures that the CSRF.getToken call
            // used in templates will find the token.
            Http.Context newCtx = new Http.WrappedContext(ctx) {
                @Override
                public RequestHeader _requestHeader() {
                    return newRequest;
                }
            };

            Http.Context.current.set(newCtx);

            // Also add it to the response
            if (config.cookieName().isDefined()) {
                Option<String> domain = Session.domain();
                ctx.response().setCookie(config.cookieName().get(), newToken, null, Session.path(),
                        domain.isDefined() ? domain.get() : null, config.secureCookie(), false);
            } else {
                ctx.session().put(config.tokenName(), newToken);
            }

            return delegate.call(newCtx);
        } else {
            return delegate.call(ctx);
        }

    }
}
