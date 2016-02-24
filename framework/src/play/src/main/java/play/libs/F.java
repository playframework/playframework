/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import play.libs.concurrent.Futures;
import play.libs.concurrent.HttpExecution;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Defines a set of functional programming style helpers.
 */
public class F {

    /**
     * A Function with 3 arguments.
     */
    public interface Function3<A,B,C,R> {
        R apply(A a, B b, C c) throws Throwable;
    }

    /**
     * A promise to produce a result of type <code>A</code>.
     *
     * @deprecated Use the JDK8 {@link CompletionStage} instead. When migrating to CompletionStage, Promise implements
     *             CompletionStage, so it may be easier to first migrate all the existing method calls on the promise,
     *             such as map/flatMap, which are also deprecated but include migration instructions in the deprecation
     *             message.
     */
    @Deprecated
    public static class Promise<A> implements CompletionStage<A> {

        private final CompletionStage<A> wrapped;

        private Promise(CompletionStage<A> wrapped) {
            this.wrapped = wrapped;
        }

        public static <A> Promise<A> wrap(CompletionStage<A> future) {
            return new Promise<>(future);
        }

        /**
         * Creates a Promise that wraps a Scala Future.
         *
         * @param future The Scala Future to wrap
         * @deprecated Use {@link FutureConverters#toJava(Future)} instead.
         */
        @Deprecated
        public static <A> Promise<A> wrap(Future<A> future) {
            return new Promise<>(FutureConverters.toJava(future));
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * The sequencing operations are performed in the default ExecutionContext.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         * @deprecated Use {@link Futures#sequence(CompletionStage[])} instead.
         */
        @Deprecated
        public static <A> Promise<List<A>> sequence(Promise<A>... promises) {
            return Futures.sequence(Arrays.asList(promises));
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param ec Used to execute the sequencing operations.
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         * @deprecated Use {@link Futures#sequence(CompletionStage[])} instead.
         */
        @Deprecated
        public static <A> Promise<List<A>> sequence(ExecutionContext ec, Promise<A>... promises) {
            return sequence(Arrays.asList(promises), ec);
        }

        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay (expressed with the corresponding unit).
         * @param unit The Unit.
         * @deprecated Use {@link Futures#timeout(Object, long, TimeUnit)} instead.
         */
        @Deprecated
        public static <A> Promise<A> timeout(A message, long delay, TimeUnit unit) {
            return Futures.timeout(message, delay, unit);
        }

        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay expressed in milliseconds.
         * @deprecated Use {@link Futures#timeout(Object, long, TimeUnit)} instead.
         */
        @Deprecated
        public static <A> Promise<A> timeout(A message, long delay) {
            return Futures.timeout(message, delay, TimeUnit.MILLISECONDS);
        }

        /**
         * Create a Promise timer that throws a PromiseTimeoutException after
         * a given timeout.
         *
         * The returned Promise is usually combined with other Promises.
         *
         * @return a promise without a real value
         * @param delay The delay expressed in milliseconds.
         * @deprecated Use {@link Futures#timeout(long, TimeUnit)} instead.
         */
        @Deprecated
        public static Promise<Void> timeout(long delay) {
            return Futures.timeout(delay, TimeUnit.MILLISECONDS);
        }

        /**
         * Create a Promise timer that throws a PromiseTimeoutException after
         * a given timeout.
         *
         * The returned Promise is usually combined with other Promises.
         *
         * @param delay The delay (expressed with the corresponding unit).
         * @param unit The Unit.
         * @return a promise without a real value
         * @deprecated Use {@link Futures#timeout(long, TimeUnit)} instead.
         */
        @Deprecated
        public static Promise<Void> timeout(long delay, TimeUnit unit) {
            return Futures.timeout(delay, unit);
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * The sequencing operations are performed in the default ExecutionContext.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         * @deprecated Use {@link Futures#sequence(Iterable)} instead.
         */
        @Deprecated
        public static <A> Promise<List<A>> sequence(Iterable<Promise<A>> promises) {
            return Futures.sequence(promises);
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param promises The promises to combine
         * @param ec Used to execute the sequencing operations.
         * @return A single promise whose methods act on the list of redeemed promises
         * @deprecated Use {@link Futures#sequence(Iterable)} instead.
         */
        @Deprecated
        public static <A> Promise<List<A>> sequence(Iterable<Promise<A>> promises, ExecutionContext ec){
            CompletableFuture<List<A>> result = CompletableFuture.completedFuture(new ArrayList<>());
            for (Promise<A> promise: promises) {
                result = result.thenCombineAsync(promise, (list, a) -> {
                    list.add(a);
                    return list;
                }, toExecutor(ec));
            }
            return new Promise<>(result);
        }

        /**
         * Create a new pure promise, that is, a promise with a constant value from the start.
         *
         * @param a the value for the promise
         * @deprecated Use {@link CompletableFuture#completedFuture(Object)} instead.
         */
        @Deprecated
        public static <A> Promise<A> pure(final A a) {
            return new Promise<>(CompletableFuture.completedFuture(a));
        }

        /**
         * Create a new promise throwing an exception.
         * @param throwable Value to throw
         * @deprecated Construct a new {@link CompletableFuture} and use
         *             {@link CompletableFuture#completeExceptionally(Throwable)} instead.
         */
        @Deprecated
        public static <A> Promise<A> throwing(Throwable throwable) {
            CompletableFuture<A> future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
            return new Promise<>(future);
        }

        /**
         * Create a Promise which will be redeemed with the result of a given function.
         *
         * The function will be run in the default ExecutionContext.
         *
         * @param function Used to fulfill the Promise.
         * @deprecated Use {@link CompletableFuture#supplyAsync(Supplier, Executor)} instead.
         */
        @Deprecated
        public static <A> Promise<A> promise(Supplier<A> function) {
            return new Promise<>(CompletableFuture.supplyAsync(function, HttpExecution.defaultContext()));
        }

        /**
         * Create a Promise which will be redeemed with the result of a given function.
         *
         * @param function Used to fulfill the Promise.
         * @param ec The ExecutionContext to run the function in.
         * @deprecated Use {@link CompletableFuture#supplyAsync(Supplier, Executor)} instead.
         */
        @Deprecated
        public static <A> Promise<A> promise(Supplier<A> function, ExecutionContext ec) {
            return new Promise<>(CompletableFuture.supplyAsync(function, toExecutor(ec)));
        }

        /**
         * Create a Promise which, after a delay, will be redeemed with the result of a
         * given function. The function will be called after the delay.
         *
         * The function will be run in the default ExecutionContext.
         *
         * @param function The function to call to fulfill the Promise.
         * @param delay The time to wait.
         * @param unit The units to use for the delay.
         * @deprecated Use {@link Futures#delayed(Supplier, long, TimeUnit, Executor)} with
         *             {@link HttpExecution#defaultContext()} instead.
         */
        @Deprecated
        public static <A> Promise<A> delayed(Supplier<A> function, long delay, TimeUnit unit) {
            return Futures.delayed(function, delay, unit, HttpExecution.defaultContext());
        }

        /**
         * Create a Promise which, after a delay, will be redeemed with the result of a
         * given function. The function will be called after the delay.
         *
         * @param function The function to call to fulfill the Promise.
         * @param delay The time to wait.
         * @param unit The units to use for the delay.
         * @deprecated Use {@link Futures#delayed(Supplier, long, TimeUnit, Executor)} instead.
         */
        @Deprecated
        public static <A> Promise<A> delayed(Supplier<A> function, long delay, TimeUnit unit, ExecutionContext ec) {
            return Futures.delayed(function, delay, unit, toExecutor(ec));
        }

        /**
         * Awaits for the promise to get the result.<br>
         * Throws a Throwable if the calculation providing the promise threw an exception
         *
         * @param timeout A user defined timeout
         * @param unit timeout for timeout
         * @throws PromiseTimeoutException when the promise did timeout.
         * @return The promised result
         * @deprecated Calling get on a promise is a blocking operation and so introduces the risk of deadlocks
         *      and has serious performance implications.
         */
        @Deprecated
        public A get(long timeout, TimeUnit unit) {
            // This rather complex exception matching is to ensure that the existing (quite comprehensive)
            // tests still pass. CompletableFuture does a lot of wrapping of exceptions, and doesn't unwrap
            // them, but this API did unwrap things, so to ensure the same exceptions are thrown from the
            // existing APIs, this needed to be done.
            try {
                return this.toCompletableFuture().get(timeout, unit);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new PromiseTimeoutException(e.getMessage(), e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else if (e.getCause() instanceof TimeoutException) {
                    throw new PromiseTimeoutException(e.getCause().getMessage(), e.getCause());
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        /**
         * Awaits for the promise to get the result.<br>
         * Throws a Throwable if the calculation providing the promise threw an exception
         *
         * @param timeout A user defined timeout in milliseconds
         * @throws PromiseTimeoutException when the promise did timeout.
         * @return The promised result
         * @deprecated Calling get on a promise is a blocking operation and so introduces the risk of deadlocks
         *      and has serious performance implications.
         */
        @Deprecated
        public A get(long timeout) {
            return get(timeout, TimeUnit.MILLISECONDS);
        }

        /**
         * Combines the current promise with <code>another</code> promise using `or`.
         *
         * @param another promise that will be combined
         * @deprecated Use {@link #applyToEither(CompletionStage, Function)} instead.
         */
        @Deprecated
        public <B> Promise<Either<A,B>> or(Promise<B> another) {
            return new Promise<>(wrapped.thenApply(Either::<A, B>Left)
                    .applyToEither(another.thenApply(Either::<A, B>Right), Function.identity()));
        }

        /**
         * Perform the given <code>action</code> callback when the Promise is redeemed.
         *
         * The callback will be run in the default execution context.
         *
         * @param action The action to perform.
         * @deprecated Use {@link #thenAcceptAsync(Consumer, Executor)} with {@link HttpExecution#defaultContext()} instead.
         */
        @Deprecated
        public void onRedeem(final Consumer<? super A> action) {
            wrapped.thenAcceptAsync(action, HttpExecution.defaultContext());
        }

        /**
         * Perform the given <code>action</code> callback when the Promise is redeemed.
         *
         * @param action The action to perform.
         * @param ec The ExecutionContext to execute the action in.
         * @deprecated Use {@link #thenAcceptAsync(Consumer, Executor)} instead.
         */
        @Deprecated
        public void onRedeem(final Consumer<? super A> action, ExecutionContext ec) {
            wrapped.thenAcceptAsync(action, toExecutor(ec));
        }

        /**
         * Maps this promise to a promise of type <code>B</code>.  The function <code>function</code> is applied as
         * soon as the promise is redeemed.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to map <code>A</code> to <code>B</code>.
         * @return A wrapped promise that maps the type from <code>A</code> to <code>B</code>.
         * @deprecated Use {@link #thenApplyAsync(Function, Executor)} with {@link HttpExecution#defaultContext()} if
         *             you want to capture the current context.
         */
        @Deprecated
        public <B> Promise<B> map(final Function<? super A, ? extends B> function) {
            return new Promise<>(wrapped.thenApplyAsync(function, HttpExecution.defaultContext()));
        }

        /**
         * Maps this promise to a promise of type <code>B</code>.  The function <code>function</code> is applied as
         * soon as the promise is redeemed.
         *
         * @param function The function to map <code>A</code> to <code>B</code>.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise that maps the type from <code>A</code> to <code>B</code>.
         * @deprecated Use {@link #thenApplyAsync(Function, Executor)}
         */
        @Deprecated
        public <B> Promise<B> map(final Function<? super A, ? extends B> function, ExecutionContext ec) {
            return new Promise<>(wrapped.thenApplyAsync(function, toExecutor(ec)));
        }

        /**
         * Wraps this promise in a promise that will handle exceptions thrown by this Promise.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to handle the exception. This may, for example, convert the exception into something
         *      of type <code>T</code>, or it may throw another exception, or it may do some other handling.
         * @return A wrapped promise that will only throw an exception if the supplied <code>function</code> throws an
         *      exception.
         * @deprecated Use {@link #exceptionally(Function)} if you don't need the current context captured,
         *             or {@link #handleAsync(BiFunction, Executor)} with {@link HttpExecution#defaultContext()} if
         *             you do.
         */
        @Deprecated
        public Promise<A> recover(final Function<Throwable, ? extends A> function) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    return function.apply(error);
                } else {
                    return a;
                }
            }, HttpExecution.defaultContext()));
        }

        /**
         * Wraps this promise in a promise that will handle exceptions thrown by this Promise.
         *
         * @param function The function to handle the exception. This may, for example, convert the exception into something
         *      of type <code>T</code>, or it may throw another exception, or it may do some other handling.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise that will only throw an exception if the supplied <code>function</code> throws an
         *      exception.
         * @deprecated Use {@link #handleAsync(BiFunction, Executor)} instead.
         */
        @Deprecated
        public Promise<A> recover(final Function<Throwable, ? extends A> function, ExecutionContext ec) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    return function.apply(error);
                } else {
                    return a;
                }
            }, toExecutor(ec)));
        }

        /**
         * Creates a new promise that will handle thrown exceptions by assigning to the value of another promise.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to handle the exception, and which returns another promise
         * @return A promise that will delegate to another promise on failure
         * @deprecated Use {@link #exceptionally(Function)} if you don't need the current context captured,
         *             or {@link #handleAsync(BiFunction, Executor)} with {@link HttpExecution#defaultContext()} if
         *             you do, then use {@link #thenCompose(Function)} with the identity function to flatten the result.
         */
        @Deprecated
        public Promise<A> recoverWith(final Function<Throwable, ? extends CompletionStage<A>> function) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    return function.apply(error);
                } else {
                    return CompletableFuture.completedFuture(a);
                }
            }, HttpExecution.defaultContext()).thenCompose(Function.identity()));
        }

        /**
         * Creates a new promise that will handle thrown exceptions by assigning to the value of another promise.
         *
         * @param function The function to handle the exception, and which returns another promise
         * @param ec The ExecutionContext to execute the function in
         * @return A promise that will delegate to another promise on failure
         * @deprecated Use {@link #handleAsync(BiFunction, Executor)} instead, followed by {@link #thenCompose(Function)}
         *             with the identity function.
         */
        @Deprecated
        public Promise<A> recoverWith(final Function<Throwable, Promise<A>> function, ExecutionContext ec) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    return function.apply(error);
                } else {
                    return CompletableFuture.completedFuture(a);
                }
            }, toExecutor(ec)).thenCompose(Function.identity()));
        }

        /**
         * Creates a new promise which holds the result of this promise if it was completed successfully,
         * otherwise the result of the {@code fallback} promise if it completed successfully.
         * If both promises failed, the resulting promise holds the throwable of this promise.
         *
         * @param fallback The promise to fallback to if this promise has failed
         * @return A promise that will delegate to another promise on failure
         * @deprecated Use {@link #handleAsync(BiFunction)} followed by {@link #thenCompose(Function)}
         *             with the identity function.
         */
        @Deprecated
        public Promise<A> fallbackTo(final Promise<A> fallback) {
            return new Promise<>(wrapped.handle((a, error) -> {
               if (error != null) {
                   return fallback.handle((fallbackA, fallbackError) -> {
                       if (fallbackError != null) {
                           CompletableFuture<A> failed = new CompletableFuture<>();
                           failed.completeExceptionally(error);
                           return failed;
                       } else {
                           return CompletableFuture.completedFuture(fallbackA);
                       }
                   }).thenCompose(Function.identity());
               } else {
                   return CompletableFuture.completedFuture(a);
               }
            }).thenCompose(Function.identity()));
        }

        /**
         * Perform the given <code>action</code> callback if the promise encounters an exception.
         *
         * This action will be run in the default exceution context.
         *
         * @param action The action to perform.
         * @deprecated Use {@link #whenCompleteAsync(BiConsumer, Executor)} with {@link HttpExecution#defaultContext()} if
         *             you want to capture the current context.
         */
        @Deprecated
        public void onFailure(final Consumer<Throwable> action) {
            wrapped.whenCompleteAsync((a, error) -> {
                if (error != null) {
                    action.accept(error);
                }
            }, HttpExecution.defaultContext());
        }

        /**
         * Perform the given <code>action</code> callback if the promise encounters an exception.
         *
         * @param action The action to perform.
         * @param ec The ExecutionContext to execute the callback in.
         * @deprecated Use {@link #whenCompleteAsync(BiConsumer, Executor)}.
         */
        @Deprecated
        public void onFailure(final Consumer<Throwable> action, ExecutionContext ec) {
            wrapped.whenCompleteAsync((a, error) -> {
                if (error != null) {
                    action.accept(error);
                }
            }, toExecutor(ec));
        }

        /**
         * Maps the result of this promise to a promise for a result of type <code>B</code>, and flattens that to be
         * a single promise for <code>B</code>.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to map <code>A</code> to a promise for <code>B</code>.
         * @return A wrapped promise for a result of type <code>B</code>
         * @deprecated Use {@link #thenComposeAsync(Function, Executor)} with {@link HttpExecution#defaultContext()} if
         *             you want to capture the current context.
         */
        @Deprecated
        public <B> Promise<B> flatMap(final Function<? super A, ? extends CompletionStage<B>> function) {
            return new Promise<>(wrapped.thenComposeAsync(function, HttpExecution.defaultContext()));
        }

        /**
         * Maps the result of this promise to a promise for a result of type <code>B</code>, and flattens that to be
         * a single promise for <code>B</code>.
         *
         * @param function The function to map <code>A</code> to a promise for <code>B</code>.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise for a result of type <code>B</code>
         * @deprecated Use {@link #thenComposeAsync(Function, Executor)}.
         */
        @Deprecated
        public <B> Promise<B> flatMap(final Function<? super A, ? extends CompletionStage<B>> function, ExecutionContext ec) {
            return new Promise<>(wrapped.thenComposeAsync(function, toExecutor(ec)));
        }

        /**
         * Creates a new promise by filtering the value of the current promise with a predicate.
         * If the predicate fails, the resulting promise will fail with a `NoSuchElementException`.
         *
         * @param predicate The predicate to test the current value.
         * @return A new promise with the current value, if the predicate is satisfied.
         * @deprecated Use {@link #thenApplyAsync(Function, Executor)} to implement the filter manually.
         */
        @Deprecated
        public Promise<A> filter(final Predicate<? super A> predicate) {
            return new Promise<>(wrapped.thenApplyAsync(a -> {
                if (predicate.test(a)) {
                    return a;
                } else {
                    throw new NoSuchElementException("Promise.filter predicate is not satisfied");
                }
            }, HttpExecution.defaultContext()));
        }

        /**
         * Creates a new promise by filtering the value of the current promise with a predicate.
         * If the predicate fails, the resulting promise will fail with a `NoSuchElementException`.
         *
         * @param predicate The predicate to test the current value.
         * @param ec The ExecutionContext to execute the filtering in.
         * @return A new promise with the current value, if the predicate is satisfied.
         * @deprecated Use {@link #thenApplyAsync(Function, Executor)} to implement the filter manually.
         */
        @Deprecated
        public Promise<A> filter(final Predicate<? super A> predicate, ExecutionContext ec) {
            return new Promise<>(wrapped.thenApplyAsync(a -> {
                if (predicate.test(a)) {
                    return a;
                } else {
                    throw new NoSuchElementException("Promise.filter predicate is not satisfied");
                }
            }, toExecutor(ec)));
        }

        /**
         * Creates a new promise by applying the {@code onSuccess} function to a successful result,
         * or the {@code onFailure} function to a failed result.
         *
         * The function will be run in the default execution context.
         *
         * @param onSuccess The function to map a successful result from {@code A} to {@code B}
         * @param onFailure The function to map the {@code Throwable} when failed
         * @return A new promise mapped by either the {@code onSuccess} or {@code onFailure} functions
         * @deprecated Use {@link #handleAsync(BiFunction, Executor)} instead.
         */
        @Deprecated
        public <B> Promise<B> transform(final Function<? super A, ? extends B> onSuccess, final Function<Throwable, Throwable> onFailure) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    throw error instanceof CompletionException ? (CompletionException) error : new CompletionException(onFailure.apply(error));
                } else {
                    return onSuccess.apply(a);
                }
            }, HttpExecution.defaultContext()));
        }

        /**
         * Creates a new promise by applying the {@code onSuccess} function to a successful result,
         * or the {@code onFailure} function to a failed result.
         *
         * @param onSuccess The function to map a successful result from {@code A} to {@code B}
         * @param onFailure The function to map the {@code Throwable} when failed
         * @param ec The ExecutionContext to execute functions in
         * @return A new promise mapped by either the {@code onSuccess} or {@code onFailure} functions
         * @deprecated Use {@link #handleAsync(BiFunction, Executor)} instead.
         */
        @Deprecated
        public <B> Promise<B> transform(final Function<? super A, ? extends B> onSuccess, final Function<Throwable, Throwable> onFailure, ExecutionContext ec) {
            return new Promise<>(wrapped.handleAsync((a, error) -> {
                if (error != null) {
                    throw error instanceof CompletionException ? (CompletionException) error : new CompletionException(onFailure.apply(error));
                } else {
                    return onSuccess.apply(a);
                }
            }, toExecutor(ec)));
        }

        /**
         * Zips the values of this promise with <code>another</code>, and creates a new promise holding the tuple of their results
         * @param another
         * @deprecated Use {@link #thenCombine(CompletionStage, BiFunction)} instead.
         */
        @Deprecated
        public <B> Promise<Tuple<A, B>> zip(CompletionStage<B> another) {
            return thenCombine(another, (a, b) -> new Tuple(a, b));
        }

        /**
         * Gets the Scala Future wrapped by this Promise.
         *
         * @return The Scala Future
         * @deprecated Promise no longer wraps a Scala Future, use asScala instead.
         */
        @Deprecated
        public Future<A> wrapped() {
            return asScala();
        }

        /**
         * Convert this promise to a Scala future.
         *
         * This is equivalent to FutureConverters.toScala(this), however, it converts the wrapped completion stage to
         * a future rather than this, which means if the wrapped completion stage itself wraps a Scala future, it will
         * simply return that wrapped future.
         *
         * @return A Scala future that is completed when this promise is completed.
         */
        public Future<A> asScala() {
            return FutureConverters.toScala(wrapped);
        }

        // delegate methods
        @Override
        public <U> Promise<U> thenApply(Function<? super A, ? extends U> fn) {
            return new Promise<>(wrapped.thenApply(fn));
        }

        @Override
        public <U> Promise<U> thenApplyAsync(Function<? super A, ? extends U> fn) {
            return new Promise<>(wrapped.thenApplyAsync(fn));
        }

        @Override
        public <U> Promise<U> thenApplyAsync(Function<? super A, ? extends U> fn, Executor executor) {
            return new Promise<>(wrapped.thenApplyAsync(fn, executor));
        }

        @Override
        public Promise<Void> thenAccept(Consumer<? super A> action) {
            return new Promise<>(wrapped.thenAccept(action));
        }

        @Override
        public Promise<Void> thenAcceptAsync(Consumer<? super A> action) {
            return new Promise<>(wrapped.thenAcceptAsync(action));
        }

        @Override
        public Promise<Void> thenAcceptAsync(Consumer<? super A> action, Executor executor) {
            return new Promise<>(wrapped.thenAcceptAsync(action, executor));
        }

        @Override
        public Promise<Void> thenRun(Runnable action) {
            return new Promise<>(wrapped.thenRun(action));
        }

        @Override
        public Promise<Void> thenRunAsync(Runnable action) {
            return new Promise<>(wrapped.thenRunAsync(action));
        }

        @Override
        public Promise<Void> thenRunAsync(Runnable action, Executor executor) {
            return new Promise<>(wrapped.thenRunAsync(action, executor));
        }

        @Override
        public <U, V> Promise<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super A, ? super U, ? extends V> fn) {
            return new Promise<>(wrapped.thenCombine(other, fn));
        }

        @Override
        public <U, V> Promise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super A, ? super U, ? extends V> fn) {
            return new Promise<>(wrapped.thenCombineAsync(other, fn));
        }

        @Override
        public <U, V> Promise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super A, ? super U, ? extends V> fn, Executor executor) {
            return new Promise<>(wrapped.thenCombineAsync(other, fn, executor));
        }

        @Override
        public <U> Promise<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super A, ? super U> action) {
            return new Promise<>(wrapped.thenAcceptBoth(other, action));
        }

        @Override
        public <U> Promise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super A, ? super U> action) {
            return new Promise<>(wrapped.thenAcceptBothAsync(other, action));
        }

        @Override
        public <U> Promise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super A, ? super U> action, Executor executor) {
            return new Promise<>(wrapped.thenAcceptBothAsync(other, action, executor));
        }

        @Override
        public Promise<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
            return new Promise<>(wrapped.runAfterBoth(other, action));
        }

        @Override
        public Promise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
            return new Promise<>(wrapped.runAfterBothAsync(other, action));
        }

        @Override
        public Promise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return new Promise<>(wrapped.runAfterBothAsync(other, action, executor));
        }

        @Override
        public <U> Promise<U> applyToEither(CompletionStage<? extends A> other, Function<? super A, U> fn) {
            return new Promise<>(wrapped.applyToEither(other, fn));
        }

        @Override
        public <U> Promise<U> applyToEitherAsync(CompletionStage<? extends A> other, Function<? super A, U> fn) {
            return new Promise<>(wrapped.applyToEitherAsync(other, fn));
        }

        @Override
        public <U> Promise<U> applyToEitherAsync(CompletionStage<? extends A> other, Function<? super A, U> fn, Executor executor) {
            return new Promise<>(wrapped.applyToEitherAsync(other, fn, executor));
        }

        @Override
        public Promise<Void> acceptEither(CompletionStage<? extends A> other, Consumer<? super A> action) {
            return new Promise<>(wrapped.acceptEither(other, action));
        }

        @Override
        public Promise<Void> acceptEitherAsync(CompletionStage<? extends A> other, Consumer<? super A> action) {
            return new Promise<>(wrapped.acceptEitherAsync(other, action));
        }

        @Override
        public Promise<Void> acceptEitherAsync(CompletionStage<? extends A> other, Consumer<? super A> action, Executor executor) {
            return new Promise<>(wrapped.acceptEitherAsync(other, action, executor));
        }

        @Override
        public Promise<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
            return new Promise<>(wrapped.runAfterEither(other, action));
        }

        @Override
        public Promise<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
            return new Promise<>(wrapped.runAfterEitherAsync(other, action));
        }

        @Override
        public Promise<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return new Promise<>(wrapped.runAfterEitherAsync(other, action, executor));
        }

        @Override
        public <U> Promise<U> thenCompose(Function<? super A, ? extends CompletionStage<U>> fn) {
            return new Promise<>(wrapped.thenCompose(fn));
        }

        @Override
        public <U> Promise<U> thenComposeAsync(Function<? super A, ? extends CompletionStage<U>> fn) {
            return new Promise<>(wrapped.thenComposeAsync(fn));
        }

        @Override
        public <U> Promise<U> thenComposeAsync(Function<? super A, ? extends CompletionStage<U>> fn, Executor executor) {
            return new Promise<>(wrapped.thenComposeAsync(fn, executor));
        }

        @Override
        public Promise<A> exceptionally(Function<Throwable, ? extends A> fn) {
            return new Promise<>(wrapped.exceptionally(fn));
        }

        @Override
        public Promise<A> whenComplete(BiConsumer<? super A, ? super Throwable> action) {
            return new Promise<>(wrapped.whenComplete(action));
        }

        @Override
        public Promise<A> whenCompleteAsync(BiConsumer<? super A, ? super Throwable> action) {
            return new Promise<>(wrapped.whenCompleteAsync(action));
        }

        @Override
        public Promise<A> whenCompleteAsync(BiConsumer<? super A, ? super Throwable> action, Executor executor) {
            return new Promise<>(wrapped.whenCompleteAsync(action, executor));
        }

        @Override
        public <U> Promise<U> handle(BiFunction<? super A, Throwable, ? extends U> fn) {
            return new Promise<>(wrapped.handle(fn));
        }

        @Override
        public <U> Promise<U> handleAsync(BiFunction<? super A, Throwable, ? extends U> fn) {
            return new Promise<>(wrapped.handleAsync(fn));
        }

        @Override
        public <U> Promise<U> handleAsync(BiFunction<? super A, Throwable, ? extends U> fn, Executor executor) {
            return new Promise<>(wrapped.handleAsync(fn, executor));
        }

        @Override
        public CompletableFuture<A> toCompletableFuture() {
            return wrapped.toCompletableFuture();
        }
    }

    /**
     * RedeemablePromise is an object which can be completed with a value or failed with an exception.
     *
     * <pre>
     * {@code
     * RedeemablePromise<Integer> someFutureInt = RedeemablePromise.empty();
     *
     * someFutureInt.map(new Function<Integer, Result>() {
     *     public Result apply(Integer i) {
     *         // This would apply once the redeemable promise succeed
     *         return Results.ok("" + i);
     *     }
     * });
     *
     * // In another thread, you now may complete the RedeemablePromise.
     * someFutureInt.success(42);
     * }
     * </pre>
     * @deprecated Use {@link CompletableFuture} instead.
     */
    @Deprecated
    public static class RedeemablePromise<A> extends Promise<A> {

        private final CompletableFuture<A> future;

        private RedeemablePromise(CompletableFuture<A> future) {
            super(future);

            this.future = future;
        }

        /**
         * Creates a new Promise with no value
         */
        public static <A> RedeemablePromise<A> empty() {
            return new RedeemablePromise<>(new CompletableFuture<>());
        }

        /**
         * Completes the promise with a value.
         *
         * @param a The value to complete with
         */
        public void success(A a) {
            if (!future.complete(a)) {
                throw new IllegalStateException("RedeemablePromise already completed.");
            }
        }

        /**
         * Completes the promise with an exception
         *
         * @param t The exception to fail the promise with
         */
        public void failure(Throwable t) {
            if (!future.completeExceptionally(t)) {
                throw new IllegalStateException("RedeemablePromise already completed.");
            }
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be a null value,
         *         if the completion failed then the result will be an IllegalStateException.
         */
        public Promise<Void> completeWith(CompletionStage<? extends A> other) {
            return new Promise<>(other.handle((a, error) -> {
                boolean completed;
                if (error != null) {
                    completed = future.completeExceptionally(error);
                } else {
                    completed = future.complete(a);
                }
                if (!completed) {
                    throw new IllegalStateException("RedeemablePromise already completed.");
                } else {
                    return null;
                }
            }));
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @param ec An execution context
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be a null value,
         *         if the completion failed then the result will be an IllegalStateException.
         */
        public Promise<Void> completeWith(CompletionStage<? extends A> other, ExecutionContext ec) {
            return new Promise<>(other.handleAsync((a, error) -> {
                boolean completed;
                if (error != null) {
                    completed = future.completeExceptionally(error);
                } else {
                    completed = future.complete(a);
                }
                if (!completed) {
                    throw new IllegalStateException("RedeemablePromise already completed.");
                } else {
                    return null;
                }
            }, toExecutor(ec)));
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be true, if the
         *         completion couldn't occur then the result will be false.
         */
        public Promise<Boolean> tryCompleteWith(Promise<? extends A> other) {
            return new Promise<>(other.handle((a, error) -> {
                if (error != null) {
                    return future.completeExceptionally(error);
                } else {
                    return future.complete(a);
                }
            }));
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @param ec An execution context
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be true, if the
         *         completion couldn't occur then the result will be false.
         */
        public Promise<Boolean> tryCompleteWith(Promise<? extends A> other, ExecutionContext ec) {
            return new Promise<>(other.handleAsync((a, error) -> {
                if (error != null) {
                    return future.completeExceptionally(error);
                } else {
                    return future.complete(a);
                }
            }, toExecutor(ec)));
        }
    }


    /**
     * Exception thrown when an operation times out. This class provides an
     * unchecked alternative to Java's TimeoutException.
     */
    public static class PromiseTimeoutException extends RuntimeException {
        public PromiseTimeoutException(String message) {
            super(message);
        }
        public PromiseTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Represents a value of one of two possible types (a disjoint union)
     */
    public static class Either<L, R> {

        /**
         * The left value.
         */
        final public Optional<L> left;

        /**
         * The right value.
         */
        final public Optional<R> right;

        private Either(Optional<L> left, Optional<R> right) {
            this.left = left;
            this.right = right;
        }

        /**
         * Constructs a left side of the disjoint union, as opposed to the Right side.
         *
         * @param value The value of the left side
         * @return A left sided disjoint union
         */
        public static <L, R> Either<L, R> Left(L value) {
            return new Either<L, R>(Optional.of(value), Optional.<R>empty());
        }

        /**
         * Constructs a right side of the disjoint union, as opposed to the Left side.
         *
         * @param value The value of the right side
         * @return A right sided disjoint union
         */
        public static <L, R> Either<L, R> Right(R value) {
            return new Either<L, R>(Optional.<L>empty(), Optional.of(value));
        }

        @Override
        public String toString() {
            return "Either(left: " + this.left + ", right: " + this.right + ")";
        }
    }

    /**
     * A pair - a tuple of the types <code>A</code> and <code>B</code>.
     */
    public static class Tuple<A, B> {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public String toString() {
            return "Tuple2(_1: " + _1 + ", _2: " + _2 + ")";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
            result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof Tuple)) return false;
            Tuple other = (Tuple) obj;
            if (_1 == null) { if (other._1 != null) return false; }
            else if (!_1.equals(other._1)) return false;
            if (_2 == null) { if (other._2 != null) return false; }
            else if (!_2.equals(other._2)) return false;
            return true;
        }
    }

    /**
     * Constructs a tuple of A,B
     *
     * @param a The a value
     * @param b The b value
     * @return The tuple
     */
    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple<A, B>(a, b);
    }

    /**
     * A tuple of A,B,C
     */
    public static class Tuple3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public Tuple3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "Tuple3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
            result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
            result = prime * result + ((_3 == null) ? 0 : _3.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof Tuple3)) return false;
            Tuple3 other = (Tuple3) obj;
            if (_1 == null) { if (other._1 != null) return false; }
            else if (!_1.equals(other._1)) return false;
            if (_2 == null) { if (other._2 != null) return false; }
            else if (!_2.equals(other._2)) return false;
            if (_3 == null) { if (other._3 != null) return false; }
            else if (!_3.equals(other._3)) return false;
            return true;
        }
    }

    /**
     * Constructs a tuple of A,B,C
     *
     * @param a The a value
     * @param b The b value
     * @param c The c value
     * @return The tuple
     */
    public static <A, B, C> Tuple3<A, B, C> Tuple3(A a, B b, C c) {
        return new Tuple3<A, B, C>(a, b, c);
    }

    /**
     * A tuple of A,B,C,D
     */
    public static class Tuple4<A, B, C, D> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;

        public Tuple4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "Tuple4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
            result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
            result = prime * result + ((_3 == null) ? 0 : _3.hashCode());
            result = prime * result + ((_4 == null) ? 0 : _4.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof Tuple4)) return false;
            Tuple4 other = (Tuple4) obj;
            if (_1 == null) { if (other._1 != null) return false; }
            else if (!_1.equals(other._1)) return false;
            if (_2 == null) { if (other._2 != null) return false; }
            else if (!_2.equals(other._2)) return false;
            if (_3 == null) { if (other._3 != null) return false; }
            else if (!_3.equals(other._3)) return false;
            if (_4 == null) { if (other._4 != null) return false; }
            else if (!_4.equals(other._4)) return false;
            return true;
        }
    }

    /**
     * Constructs a tuple of A,B,C,D
     *
     * @param a The a value
     * @param b The b value
     * @param c The c value
     * @param d The d value
     * @return The tuple
     */
    public static <A, B, C, D> Tuple4<A, B, C, D> Tuple4(A a, B b, C c, D d) {
        return new Tuple4<A, B, C, D>(a, b, c, d);
    }

    /**
     * A tuple of A,B,C,D,E
     */
    public static class Tuple5<A, B, C, D, E> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;

        public Tuple5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "Tuple5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
            result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
            result = prime * result + ((_3 == null) ? 0 : _3.hashCode());
            result = prime * result + ((_4 == null) ? 0 : _4.hashCode());
            result = prime * result + ((_5 == null) ? 0 : _5.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof Tuple5)) return false;
            Tuple5 other = (Tuple5) obj;
            if (_1 == null) { if (other._1 != null) return false; }
            else if (!_1.equals(other._1)) return false;
            if (_2 == null) { if (other._2 != null) return false; }
            else if (!_2.equals(other._2)) return false;
            if (_3 == null) { if (other._3 != null) return false; }
            else if (!_3.equals(other._3)) return false;
            if (_4 == null) { if (other._4 != null) return false; }
            else if (!_4.equals(other._4)) return false;
            if (_5 == null) { if (other._5 != null) return false; }
            else if (!_5.equals(other._5)) return false;
            return true;
        }
    }

    /**
     * Constructs a tuple of A,B,C,D,E
     *
     * @param a The a value
     * @param b The b value
     * @param c The c value
     * @param d The d value
     * @param e The e value
     * @return The tuple
     */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> Tuple5(A a, B b, C c, D d, E e) {
        return new Tuple5<A, B, C, D, E>(a, b, c, d, e);
    }

    /**
     * Converts the execution context to an executor, preparing it first.
     */
    private static Executor toExecutor(ExecutionContext ec) {
        ExecutionContext prepared = ec.prepare();
        if (prepared instanceof Executor) {
            return (Executor) prepared;
        } else {
            return prepared::execute;
        }
    }

}
