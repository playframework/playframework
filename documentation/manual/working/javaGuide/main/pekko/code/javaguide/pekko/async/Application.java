/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.async;

// #async
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.mvc.*;

public class Application extends Controller {
  public CompletionStage<Result> index() {
    return CompletableFuture.supplyAsync(this::longComputation)
        .thenApply((Integer i) -> ok("Got " + i));
  }
  // ###skip: 3
  public int longComputation() {
    return 2;
  }
}
// #async
