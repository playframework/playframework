package play.filters.csrf;

import play.api.mvc.RequestHeader;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.SimpleResult;
import scala.Option;

public class RequireCSRFCheckAction extends Action<RequireCSRFCheck> {

    private final String tokenName = CSRFConf$.MODULE$.TokenName();
    private final Option<String> cookieName = CSRFConf$.MODULE$.CookieName();
    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;
    private final CSRF.TokenProvider tokenProvider = CSRFConf$.MODULE$.defaultTokenProvider();

    @Override
    public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
        RequestHeader request = ctx._requestHeader();
        // Check for bypass
        if (CSRFAction.checkCsrfBypass(request)) {
            return delegate.call(ctx);
        } else {
            // Get token from cookie/session
            Option<String> headerToken = CSRFAction.getTokenFromHeader(request, tokenName, cookieName);
            if (headerToken.isDefined()) {
                String tokenToCheck = null;

                // Get token from query string
                Option<String> queryStringToken = CSRFAction.getTokenFromQueryString(request, tokenName);
                if (queryStringToken.isDefined()) {
                    tokenToCheck = queryStringToken.get();
                } else {

                    // Get token from body
                    if (ctx.request().body().asFormUrlEncoded() != null) {
                        String[] values = ctx.request().body().asFormUrlEncoded().get(tokenName);
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    } else if (ctx.request().body().asMultipartFormData() != null) {
                        String[] values = ctx.request().body().asMultipartFormData().asFormUrlEncoded().get(tokenName);
                        if (values != null && values.length > 0) {
                            tokenToCheck = values[0];
                        }
                    }
                }

                if (tokenToCheck != null) {
                    if (tokenProvider.compareTokens(tokenToCheck, headerToken.get())) {
                        return delegate.call(ctx);
                    } else {
                        return F.Promise.pure((SimpleResult) forbidden("CSRF tokens don't match"));
                    }
                } else {
                    return F.Promise.pure((SimpleResult) forbidden("CSRF token not found in body or query string"));
                }
            } else {
                return F.Promise.pure((SimpleResult) forbidden("CSRF token not found in session"));
            }
        }
    }
}
