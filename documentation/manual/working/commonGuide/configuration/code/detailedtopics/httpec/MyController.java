/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.httpec;

//#http-execution-context
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.*;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MyController extends Controller {

    private HttpExecutionContext httpExecutionContext;

    @Inject
    public MyController(HttpExecutionContext ec) {
        this.httpExecutionContext = ec;
    }

    public CompletionStage<Result> index() {
        // Use a different task with explicit EC
        return calculateResponse().thenApplyAsync(answer -> {
            return ok("answer was " + answer).flashing("info", "Response updated!");
        }, httpExecutionContext.current());
    }

    private static CompletionStage<String> calculateResponse() {
        return CompletableFuture.completedFuture("42");
    }
}
//#http-execution-context
