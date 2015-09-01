package play.mvc;

import akka.util.ByteString;
import play.api.http.Status$;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.libs.streams.Accumulator;
import scala.compat.java8.FutureConverters;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Utilities for creating body parsers.
 */
public class BodyParsers {

    /**
     * Validate the content type of the passed in request using the given validator.
     *
     * If the validator returns true, the passed in accumulator will be returned to parse the body, otherwise an
     * accumulator with a result created by the error handler will be returned.
     *
     * @param errorHandler The error handler used to create a bad request result if the content type is not valid.
     * @param request The request to validate.
     * @param errorMessage The error message to pass to the error handler if the content type is not valid.
     * @param validate The validation function.
     * @param parser The parser to use if the content type is valid.
     * @return An accumulator to parse tho body.
     */
    public static <A> Accumulator<ByteString, F.Either<Result, A>> validateContentType(HttpErrorHandler errorHandler,
               Http.RequestHeader request, String errorMessage, Function<String, Boolean> validate,
               Function<Http.RequestHeader, Accumulator<ByteString, F.Either<Result, A>>> parser) {
        if (request.contentType().map(validate).orElse(false)) {
            return parser.apply(request);
        } else {
            CompletionStage<Result> result = FutureConverters.toJava(
                    errorHandler.onClientError(request, Status$.MODULE$.UNSUPPORTED_MEDIA_TYPE(), errorMessage).wrapped()
            );
            return Accumulator.done(result.thenApply(F.Either::Left));
        }
    }

}
