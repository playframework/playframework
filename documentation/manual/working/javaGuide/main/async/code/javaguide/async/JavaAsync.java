/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Results.ok;

import java.time.Duration;
import java.util.concurrent.*;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import play.libs.concurrent.*;
import play.mvc.Result;

public class JavaAsync {

  @Test
  void promiseWithTimeout() throws Exception {
    // #timeout
    class MyClass {

      private final Futures futures;
      private final Executor customExecutor = ForkJoinPool.commonPool();

      @Inject
      public MyClass(Futures futures) {
        this.futures = futures;
      }

      CompletionStage<Double> callWithOneSecondTimeout() {
        return futures.timeout(computePIAsynchronously(), Duration.ofSeconds(1));
      }

      public CompletionStage<String> delayedResult() {
        long start = System.currentTimeMillis();
        return futures.delayed(
            () ->
                CompletableFuture.supplyAsync(
                    () -> {
                      long end = System.currentTimeMillis();
                      long seconds = end - start;
                      return "rendered after " + seconds + " seconds";
                    },
                    customExecutor),
            Duration.of(3, SECONDS));
      }
    }
    // #timeout
    Futures futures = mock(Futures.class);
    when(futures.timeout(any(), any())).thenReturn(CompletableFuture.completedFuture(Math.PI));
    final Double actual =
        new MyClass(futures)
            .callWithOneSecondTimeout()
            .toCompletableFuture()
            .get(1, TimeUnit.SECONDS);
    final Double expected = Math.PI;
    assertEquals(expected, actual);
  }

  @Test
  public void promisePi() throws Exception {
    // #promise-pi
    CompletionStage<Double> promiseOfPIValue = computePIAsynchronously();
    // Runs in same thread
    CompletionStage<Result> promiseOfResult =
        promiseOfPIValue.thenApply(pi -> ok("PI value computed: " + pi));
    // #promise-pi
    assertEquals(200, promiseOfResult.toCompletableFuture().get(1, TimeUnit.SECONDS).status());
  }

  @Test
  public void promiseAsync() throws Exception {
    // #promise-async
    // creates new task
    CompletionStage<Integer> promiseOfInt =
        CompletableFuture.supplyAsync(() -> intensiveComputation());
    // #promise-async
    assertEquals(
        intensiveComputation(), promiseOfInt.toCompletableFuture().get(1, TimeUnit.SECONDS));
  }

  private static CompletionStage<Double> computePIAsynchronously() {
    return CompletableFuture.completedFuture(Math.PI);
  }

  private static Integer intensiveComputation() {
    return 1 + 1;
  }
}
