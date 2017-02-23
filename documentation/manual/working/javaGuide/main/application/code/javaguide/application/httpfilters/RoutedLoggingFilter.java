/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.application.httpfilters;

// #routing-info-access
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.Map;
import javax.inject.Inject;
import akka.stream.Materializer;
import play.Logger;
import play.mvc.*;
import play.routing.Router.Tags;

public class RoutedLoggingFilter extends Filter {

    @Inject
    public RoutedLoggingFilter(Materializer mat) {
        super(mat);
    }

    @Override
    public CompletionStage<Result> apply(
            Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
            Http.RequestHeader requestHeader) {
        long startTime = System.currentTimeMillis();
        return nextFilter.apply(requestHeader).thenApply(result -> {
            Map<String, String> tags = requestHeader.tags();
            String actionMethod = tags.get(Tags.ROUTE_CONTROLLER) +
                "." + tags.get(Tags.ROUTE_ACTION_METHOD);
            long endTime = System.currentTimeMillis();
            long requestTime = endTime - startTime;

            Logger.info("{} took {}ms and returned {}",
                actionMethod, requestTime, result.status());

            return result.withHeader("Request-Time", "" + requestTime);
        });
    }
}
// #routing-info-access
