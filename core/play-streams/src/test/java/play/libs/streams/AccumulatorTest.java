/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.streams;

import static org.junit.Assert.*;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.junit.*;
import org.reactivestreams.Subscription;

public class AccumulatorTest {

  private Materializer mat;
  private ActorSystem system;
  private Executor ec;

  private Accumulator<Integer, Integer> sum =
      Accumulator.fromSink(Sink.<Integer, Integer>fold(0, Integer::sum));
  private Source<Integer, ?> source = Source.from(Arrays.asList(1, 2, 3));

  private <T> T await(CompletionStage<T> cs) throws Exception {
    // thenApply is needed because https://github.com/scala/scala-java8-compat/issues/43
    return cs.thenApply(Function.identity()).toCompletableFuture().get();
  }

  private <T> Function<Object, T> error() {
    return (any) -> {
      throw new RuntimeException("error");
    };
  }

  private <T> Source<T, ?> errorSource() {
    return Source.fromPublisher(
        s ->
            s.onSubscribe(
                new Subscription() {
                  public void request(long n) {
                    s.onError(new RuntimeException("error"));
                  }

                  public void cancel() {}
                }));
  }

  @Test
  public void map() throws Exception {
    assertEquals(16, (int) await(sum.map(s -> s + 10, ec).run(source, mat)));
  }

  @Test
  public void mapFuture() throws Exception {
    assertEquals(
        16,
        (int)
            await(
                sum.mapFuture(s -> CompletableFuture.completedFuture(s + 10), ec)
                    .run(source, mat)));
  }

  @Test
  public void recoverMaterializedException() throws Exception {
    assertEquals(20, (int) await(sum.map(this.error(), ec).recover(t -> 20, ec).run(source, mat)));
  }

  @Test
  public void recoverStreamException() throws Exception {
    assertEquals(20, (int) await(sum.recover(t -> 20, ec).run(errorSource(), mat)));
  }

  @Test
  public void recoverWithMaterializedException() throws Exception {
    assertEquals(
        20,
        (int)
            await(
                sum.map(this.error(), ec)
                    .recoverWith(t -> CompletableFuture.completedFuture(20), ec)
                    .run(source, mat)));
  }

  @Test
  public void recoverWithStreamException() throws Exception {
    assertEquals(
        20,
        (int)
            await(
                sum.recoverWith(t -> CompletableFuture.completedFuture(20), ec)
                    .run(errorSource(), mat)));
  }

  @Test
  public void through() throws Exception {
    assertEquals(
        12, (int) await(sum.through(Flow.<Integer>create().map(i -> i * 2)).run(source, mat)));
  }

  @Before
  public void setUp() {
    system = ActorSystem.create();
    mat = Materializer.matFromSystem(system);
    ec = system.dispatcher();
  }

  @After
  public void tearDown() {
    system.terminate();
  }
}
