package play.libs.streams;

import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Accumulates inputs asynchronously into an output value.
 *
 * An accumulator is a view over an Akka streams Sink that materialises to a future, that is focused on the value of
 * that future, rather than the Stream. This means methods such as <code>map</code>, <code>recover</code> and so on are
 * provided for the the eventually redeemed future value.
 *
 * In order to be in line with the Java ecosystem, the future implementation that this uses for the materialised value
 * of the Sink is java.util.concurrent.CompletionStage, and running this accumulator will yield a CompletionStage. The
 * constructor allows an accumulator to be created from such a sink. Many methods in the Akka streams API however
 * materialise a scala.concurrent.Future, hence the <code>fromSink</code> method is provided to create an accumulator
 * from a typical Akka streams <code>Sink</code>.
 */
public final class Accumulator<E, A> {

    private final Sink<E, CompletionStage<A>> sink;

    /**
     * Create an accumulator for the given sink.
     *
     * @param sink The sink to wrap.
     */
    public Accumulator(Sink<E, CompletionStage<A>> sink) {
        this.sink = sink;
    }

    /**
     * Map the accumulated value.
     *
     * @param f The function to perform the map with.
     * @param executor The executor to run the function in.
     * @return A new accumulator with the mapped value.
     */
    public <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor) {
        return new Accumulator<>(sink.mapMaterializedValue(cs -> cs.thenApplyAsync(f, executor)));
    }

    /**
     * Map the accumulated value with a function that returns a future.
     *
     * @param f The function to perform the map with.
     * @param executor The executor to run the function in.
     * @return A new accumulator with the mapped value.
     */
    public <B> Accumulator<E, B> mapFuture(Function<? super A, ? extends CompletionStage<B>> f, Executor executor) {
        return new Accumulator<>(sink.mapMaterializedValue(cs -> cs.thenComposeAsync(f, executor)));
    }

    /**
     * Recover from any errors encountered by the accumulator.
     *
     * @param f The function to use to recover from errors.
     * @param executor The executor to run the function in.
     * @return A new accumulator that has recovered from errors.
     */
    public Accumulator<E, A> recover(Function<? super Throwable, ? extends A> f, Executor executor) {
        return new Accumulator<>(
                sink.mapMaterializedValue(cs ->
                        cs.handleAsync((a, error) -> {
                            if (a != null) {
                                return a;
                            } else {
                                return f.apply(error);
                            }
                        }, executor))
        );
    }

    /**
     * Recover from any errors encountered by the accumulator.
     *
     * @param f The function to use to recover from errors.
     * @param executor The executor to run the function in.
     * @return A new accumulator that has recovered from errors.
     */
    public Accumulator<E, A> recoverWith(Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor) {
        // Below is the way that this *should* be implemented, but doesn't work due to
        // https://github.com/scala/scala-java8-compat/issues/29
        //        return new Accumulator<>(
        //                sink.mapMaterializedValue(cs ->
        //                        cs.handleAsync((a, error) -> {
        //                            if (a != null) {
        //                                return CompletableFuture.completedFuture(a);
        //                            } else {
        //                                if (error instanceof CompletionException) {
        //                                    return f.apply(error.getCause());
        //                                } else {
        //                                    return f.apply(error);
        //                                }
        //                            }
        //                        }, executor).thenCompose(Function.identity()))
        //        );
        return new Accumulator<>(
                sink.mapMaterializedValue(cs -> {
                    CompletableFuture<A> future = new CompletableFuture<>();
                    cs.whenCompleteAsync((a, error) -> {
                        if (a != null) {
                            future.complete(a);
                        } else {
                            try {
                                CompletionStage<A> recovered;
                                if (error instanceof CompletionException) {
                                    recovered = f.apply(error.getCause());
                                } else {
                                    recovered = f.apply(error);
                                }
                                recovered.thenAccept(future::complete);
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        }
                    }, executor);
                    return future;
                })
        );

    }

    /**
     * Pass the stream through the given flow before forwarding it to the accumulator.
     *
     * @param flow The flow to send the stream through first.
     * @return A new accumulator with the given flow in its graph.
     */
    public <D> Accumulator<D, A> through(Flow<D, E, ?> flow) {
        return new Accumulator<>(flow.toMat(sink, Keep.right()));
    }

    /**
     * Run the accumulator with an empty source.
     *
     * @param mat The flow materializer.
     * @return A future that will be redeemed when the accumulator is done.
     */
    public CompletionStage<A> run(Materializer mat) {
        return Source.<E>empty().runWith(sink, mat);
    }

    /**
     * Run the accumulator with the given source.
     *
     * @param source The source to feed into the accumulator.
     * @param mat The flow materializer.
     * @return A fuwure that will be redeemed when the accumulator is done.
     */
    public CompletionStage<A> run(Source<E, ?> source, Materializer mat) {
        return source.runWith(sink, mat);
    }

    /**
     * Convert this accumulator to a sink.
     *
     * @return The sink.
     */
    public Sink<E, CompletionStage<A>> toSink() {
        return sink;
    }

    /**
     * Convert this accumulator to a Scala accumulator.
     *
     * @return The Scala Accumulator.
     */
    public play.api.libs.streams.Accumulator<E, A> asScala() {
        return new play.api.libs.streams.Accumulator<>(sink.mapMaterializedValue(FutureConverters::toScala).asScala());
    }

    /**
     * Create an accumulator from an Akka streams sink.
     *
     * The passed in Sink must materialize to a Scala Future. This is useful for when working with the built in Akka
     * streams Sinks, such as Sink.fold, which materialize to Scala Futures.
     *
     * @param sink The sink.
     * @return An accumulator created from the sink.
     */
    public static <E, A> Accumulator<E, A> fromSink(Sink<E, Future<A>> sink) {
        return new Accumulator<>(sink.mapMaterializedValue(FutureConverters::toJava));
    }

    /**
     * Create a done accumulator with the given value.
     *
     * @param a The done value for the accumulator.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(A a) {
        return done(CompletableFuture.completedFuture(a));
    }

    /**
     * Create a done accumulator with the given future.
     *
     * @param a A future of the done value.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(CompletionStage<A> a) {
        return new Accumulator(Sink.cancelled().mapMaterializedValue((unit) -> a));
    }

}
