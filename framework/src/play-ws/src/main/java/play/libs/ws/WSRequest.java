package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.mvc.Http;

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

}
