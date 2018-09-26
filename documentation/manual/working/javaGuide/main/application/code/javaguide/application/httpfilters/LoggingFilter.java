/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.application.httpfilters;

// #simple-filter
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.*;

public class LoggingFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Inject
    public LoggingFilter(Materializer mat) {
        super(mat);
    }

    @Override
    public CompletionStage<Result> apply(
            Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
            Http.RequestHeader requestHeader) {
        long startTime = System.currentTimeMillis();
        return nextFilter.apply(requestHeader).thenApply(result -> {
            long endTime = System.currentTimeMillis();
            long requestTime = endTime - startTime;

            log.info("{} {} took {}ms and returned {}",
                requestHeader.method(), requestHeader.uri(), requestTime, result.status());

            return result.withHeader("Request-Time", "" + requestTime);
        });
    }
}
// #simple-filter
