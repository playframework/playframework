package play.libs;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.http.HttpRequest;
import play.libs.WS.SignatureCalculator;
import play.libs.WS.WSRequest;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class OAuth {

    private ServiceInfo info;
    private OAuthProvider provider;

    public OAuth(ServiceInfo info) {
        this(info, true);
    }

    public OAuth(ServiceInfo info, boolean use10a) {
        this.info = info;
        this.provider = new CommonsHttpOAuthProvider(info.requestTokenURL, info.accessTokenURL, info.authorizationURL);
        this.provider.setOAuth10a(use10a);
    }

    public ServiceInfo getInfo() {
        return info;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    /**
     * Request the request token and secret.
     *
     * @param callbackURL the URL where the provider should redirect to (usually a URL on the current app)
     * @return A Right(RequestToken) in case of success, Left(OAuthException) otherwise
     */
    public RequestToken retrieveRequestToken(String callbackURL) {
        OAuthConsumer consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret);
        try {
            provider.retrieveRequestToken(consumer, callbackURL);
            return new RequestToken(consumer.getToken(), consumer.getTokenSecret());
        } catch(OAuthException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Exchange a request token for an access token.
     *
     * @param token the token/secret pair obtained from a previous call
     * @param verifier a string you got through your user, with redirection
     * @return A Right(RequestToken) in case of success, Left(OAuthException) otherwise
     */
    public RequestToken retrieveAccessToken(RequestToken token, String verifier) {
        OAuthConsumer consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret);
        consumer.setTokenWithSecret(token.token, token.secret);
        try {
            provider.retrieveAccessToken(consumer, verifier);
            return new RequestToken(consumer.getToken(), consumer.getTokenSecret());
        } catch (OAuthException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * The URL where the user needs to be redirected to grant authorization to your application.
     *
     * @param token request token
     */
    public String redirectUrl(String token) {
        return oauth.signpost.OAuth.addQueryParameters(
            provider.getAuthorizationWebsiteUrl(),
            oauth.signpost.OAuth.OAUTH_TOKEN,
            token
        );
    }

    /**
     * A consumer key / consumer secret pair that the OAuth provider gave you, to identify your application.
     */
    public static class ConsumerKey {
        public String key;
        public String secret;
        public ConsumerKey(String key, String secret) {
            this.key = key;
            this.secret = secret;
        }
    }

    /**
     * A request token / token secret pair, to be used for a specific user.
     */
    public static class RequestToken {
        public String token;
        public String secret;
        public RequestToken(String token, String secret) {
            this.token = token;
            this.secret = secret;
        }
    }

    /**
     * The information identifying a oauth provider: URLs and the consumer key / consumer secret pair.
     */
    public static class ServiceInfo {
        public String requestTokenURL;
        public String accessTokenURL;
        public String authorizationURL;
        public ConsumerKey key;
        public ServiceInfo(String requestTokenURL, String accessTokenURL, String authorizationURL, ConsumerKey key) {
            this.requestTokenURL = requestTokenURL;
            this.accessTokenURL = accessTokenURL;
            this.authorizationURL = authorizationURL;
            this.key = key;
        }
    }

    /**
     * A signature calculator for the Play WS API.
     *
     * Example:
     * {{{
     * WS.url("http://example.com/protected").sign(OAuthCalculator(service, tokens)).get()
     * }}}
     */
    public static class OAuthCalculator extends AbstractOAuthConsumer implements SignatureCalculator {

        public OAuthCalculator(ConsumerKey consumerKey, RequestToken token) {
            super(consumerKey.key, consumerKey.secret);
            this.setTokenWithSecret(token.token, token.secret);
        }

        @Override
        protected HttpRequest wrap(Object request) {
            if (request instanceof WSRequest) {
                return new WSRequestAdapter((WSRequest)request);
            } else {
                throw new IllegalArgumentException("OAuthCalculator expects requests of type play.libs.WS.WSRequest");
            }
        }

        @Override
        public void sign(WSRequest request) {
            try {
                sign(wrap(request));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class WSRequestAdapter implements HttpRequest {

        private WSRequest request;

        public WSRequestAdapter(WSRequest request) {
            this.request = request;
        }

        @Override
        public Object unwrap() {
            return request;
        }

        @Override
        public Map<String, String> getAllHeaders() {
            return Maps.transformValues(request.getAllHeaders(), new Function<List<String>, String>(){
                @Override public String apply(List<String> values) {
                    if (values.size() == 0) return "";
                    return values.get(values.size() - 1);
                }});
        }

        @Override
        public String getHeader(String name) {
            List<String> values = request.getHeader(name);
            if (values.size() == 0) return "";
            return values.get(values.size() - 1);
        }

        @Override
        public String getContentType() {
            return getHeader("Content-Type");
        }

        @Override
        public InputStream getMessagePayload() {
            return null;
        }

        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public void setHeader(String name, String value) {
            request.setHeader(name, value);
        }

        @Override
        public String getRequestUrl() {
            return request.getUrl();
        }

        @Override
        public void setRequestUrl(String url) {
            request.setUrl(url);
        }

    }

}
