/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.application.httpfilters;

// #essential-filter-example
import java.util.concurrent.Executor;
import javax.inject.Inject;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.streams.Accumulator;
import play.mvc.*;

public class EssentialLoggingFilter extends EssentialFilter {

    private static final Logger log = LoggerFactory.getLogger(EssentialLoggingFilter.class);

    private final Executor executor;

    @Inject
    public EssentialLoggingFilter(Executor executor) {
        super();
        this.executor = executor;
    }

    @Override
    public EssentialAction apply(EssentialAction next) {
        return EssentialAction.of(request -> {
            long startTime = System.currentTimeMillis();
            Accumulator<ByteString, Result> accumulator = next.apply(request);
            return accumulator.map(result -> {
                long endTime = System.currentTimeMillis();
                long requestTime = endTime - startTime;

                log.info("{} {} took {}ms and returned {}",
                    request.method(), request.uri(), requestTime, result.status());

                return result.withHeader("Request-Time", "" + requestTime);
            }, executor);
        });
    }
}
// #essential-filter-example
