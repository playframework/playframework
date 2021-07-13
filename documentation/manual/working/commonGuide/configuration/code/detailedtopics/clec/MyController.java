/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.clec;

// #cl-execution-context
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.*;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MyController extends Controller {

  private ClassLoaderExecutionContext clExecutionContext;

  @Inject
  public MyController(ClassLoaderExecutionContext ec) {
    this.clExecutionContext = ec;
  }

  public CompletionStage<Result> index() {
    // Use a different task with explicit EC
    return calculateResponse()
        .thenApplyAsync(
            answer -> {
              return ok("answer was " + answer).flashing("info", "Response updated!");
            },
            clExecutionContext.current());
  }

  private static CompletionStage<String> calculateResponse() {
    return CompletableFuture.completedFuture("42");
  }
}
// #cl-execution-context
