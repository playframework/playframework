package play.filters.csrf;

import play.api.mvc.RequestHeader;
import play.api.mvc.Session;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.SimpleResult;
import scala.Option;
import scala.Tuple2;

public class AddCSRFTokenAction extends Action<AddCSRFToken> {

    private final String tokenName = CSRFConf$.MODULE$.TokenName();
    private final Option<String> cookieName = CSRFConf$.MODULE$.CookieName();
    private final boolean secureCookie = CSRFConf$.MODULE$.SecureCookie();
    private final String requestTag = CSRF.Token$.MODULE$.RequestTag();
    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;
    private final CSRF.TokenProvider tokenProvider = CSRFConf$.MODULE$.defaultTokenProvider();

    @Override
    public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
        RequestHeader request = ctx._requestHeader();

        if (CSRFAction.getTokenFromHeader(request, tokenName, cookieName).isEmpty()) {
            // No token in header and we have to create one if not found, so create a new token
            String newToken = tokenProvider.generateToken();

            // Place this token into the context
            ctx.args.put(requestTag, newToken);

            // Create a new Scala RequestHeader with the token
            RequestHeader newRequest = request.copy(request.id(),
                    request.tags().$plus(new Tuple2<String, String>(requestTag, newToken)),
                    request.uri(), request.path(), request.method(), request.version(), request.queryString(),
                    request.headers(), request.remoteAddress());

            // Create a new context that will have the new RequestHeader.  This ensures that the CSRF.getToken call
            // used in templates will find the token.
            Http.Context newCtx = new Http.Context(ctx.id(), newRequest, ctx.request(), ctx.session(), ctx.flash(),
                    ctx.args);
            Http.Context.current.set(newCtx);

            // Also add it to the response
            if (cookieName.isDefined()) {
                Option<String> domain = Session.domain();
                ctx.response().setCookie(cookieName.get(), newToken, null, Session.path(),
                        domain.isDefined() ? domain.get() : null, secureCookie, false);
            } else {
                ctx.session().put(tokenName, newToken);
            }

            return delegate.call(newCtx);
        } else {
            return delegate.call(ctx);
        }

    }
}
