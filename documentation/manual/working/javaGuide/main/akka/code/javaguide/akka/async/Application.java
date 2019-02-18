/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka.async;

// #async
import play.mvc.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Application extends Controller {
  public CompletionStage<Result> index() {
    return CompletableFuture.supplyAsync(this::longComputation)
        .thenApply((Integer i) -> ok("Got " + i));
  }
  // ###skip: 1
  public int longComputation() {
    return 2;
  }
}
// #async
