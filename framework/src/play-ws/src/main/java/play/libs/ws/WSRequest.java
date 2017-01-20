package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Http;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CompletionStage;

/**
 * An enhanced WSRequest that can use Play specific classes.
 */
public interface WSRequest extends StandaloneWSRequest {

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    /**
     * Set the multipart body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setMultipartBody(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);


    //-------------------------------------------------------------------------
    // "GET"
    //-------------------------------------------------------------------------

    /**
     * Perform a GET on the request asynchronously.
     *
     * @return a promise to the response
     */
    CompletionStage<WSResponse> get();

    //-------------------------------------------------------------------------
    // "PATCH"
    //-------------------------------------------------------------------------

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(String body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(JsonNode body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(InputStream body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<? extends WSResponse> patch(File body);

    //-------------------------------------------------------------------------
    // "POST"
    //-------------------------------------------------------------------------

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(String body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(JsonNode body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(InputStream body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(File body);

    //-------------------------------------------------------------------------
    // "PUT"
    //-------------------------------------------------------------------------

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(String body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(JsonNode body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(InputStream body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(File body);

    //-------------------------------------------------------------------------
    // Miscellaneous execution methods
    //-------------------------------------------------------------------------

    /**
     * Perform a DELETE on the request asynchronously.
     *
     * @return a promise to the response
     */
    CompletionStage<WSResponse> delete();

    /**
     * Perform a HEAD on the request asynchronously.
     *
     * @return a promise to the response
     */
    CompletionStage<WSResponse> head();

    /**
     * Perform an OPTIONS on the request asynchronously.
     *
     * @return a promise to the response
     */
    CompletionStage<WSResponse> options();

    /**
     * Execute an arbitrary method on the request asynchronously.
     *
     * @param method The method to execute
     * @return a promise to the response
     */
    CompletionStage<WSResponse> execute(String method);

    /**
     * Execute an arbitrary method on the request asynchronously.  Should be used with setMethod().
     *
     * @return a promise to the response
     */
    CompletionStage<WSResponse> execute();

    /**
     * Execute this request and stream the response body.
     *
     * @return a promise to the streaming response
     */
    CompletionStage<? extends StreamedResponse> stream();

    //-------------------------------------------------------------------------
    // Setters
    //-------------------------------------------------------------------------

    /**
     * Sets the HTTP method this request should use, where the no args execute() method is invoked.
     *
     * @param method the HTTP method.
     * @return the modified WSRequest.
     */
    WSRequest setMethod(String method);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(String body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(JsonNode body);

    /**
     * Set the body this request should use.
     *
     * @param body Deprecated
     * @return Deprecated
     * @deprecated use {@link #setBody(Source)} instead.
     */
    @Deprecated
    WSRequest setBody(InputStream body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(File body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    <U> WSRequest setBody(Source<ByteString, U> body);

    /**
     * Adds a header to the request.  Note that duplicate headers are allowed
     * by the HTTP specification, and removing a header is not available
     * through this API.
     *
     * @param name  the header name
     * @param value the header value
     * @return the modified WSRequest.
     */
    WSRequest setHeader(String name, String value);

    /**
     * Sets the query string to query.
     *
     * @param query the fully formed query string
     * @return the modified WSRequest.
     */
    WSRequest setQueryString(String query);

    /**
     * Sets a query parameter with the given name, this can be called repeatedly.  Duplicate query parameters are allowed.
     *
     * @param name  the query parameter name
     * @param value the query parameter value
     * @return the modified WSRequest.
     */
    WSRequest setQueryParameter(String name, String value);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param userInfo a string formed as "username:password".
     * @return the modified WSRequest.
     */
    WSRequest setAuth(String userInfo);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param username the basic auth username
     * @param password the basic auth password
     * @return the modified WSRequest.
     */
    WSRequest setAuth(String username, String password);

    /**
     * Sets the authentication header for the current request.
     *
     * @param username the username
     * @param password the password
     * @param scheme   authentication scheme
     * @return the modified WSRequest.
     */
    WSRequest setAuth(String username, String password, WSAuthScheme scheme);

    /**
     * Sets an (OAuth) signature calculator.
     *
     * @param calculator the signature calculator
     * @return the modified WSRequest
     */
    WSRequest sign(WSSignatureCalculator calculator);

    /**
     * Sets whether redirects (301, 302) should be followed automatically.
     *
     * @param followRedirects true if the request should follow redirects
     * @return the modified WSRequest
     */
    WSRequest setFollowRedirects(boolean followRedirects);

    /**
     * Sets the virtual host as a "hostname:port" string.
     *
     * @param virtualHost the virtual host
     * @return the modified WSRequest
     */
    WSRequest setVirtualHost(String virtualHost);

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param timeout the request timeout in milliseconds. A value of -1 indicates an infinite request timeout.
     * @return the modified WSRequest.
     */
    WSRequest setRequestTimeout(long timeout);

    /**
     * Adds a request filter.
     *
     * @param filter a transforming filter.
     * @return the modified request.
     */
    WSRequest setRequestFilter(WSRequestFilter filter);

    /**
     * Set the content type.  If the request body is a String, and no charset parameter is included, then it will
     * default to UTF-8.
     *
     * @param contentType The content type
     * @return the modified WSRequest
     */
    WSRequest setContentType(String contentType);

}
