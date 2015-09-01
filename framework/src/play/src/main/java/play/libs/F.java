/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import java.util.function.Function;
import java.util.function.Supplier;

import play.core.j.FPromiseHelper;
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
     */
    public static class Promise<A> {

        private final Future<A> future;

        /**
         * Creates a Promise that wraps a Scala Future.
         *
         * @param future The Scala Future to wrap
         */
        private Promise(Future<A> future) {
            this.future = future;
        }

        /**
         * Creates a Promise that wraps a Scala Future.
         *
         * @param future The Scala Future to wrap
         */
        @SuppressWarnings("deprecation")
        public static <A> Promise<A> wrap(Future<A> future) {
            return new Promise<A>(future);
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * The sequencing operations are performed in the default ExecutionContext.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> sequence(Promise<A>... promises){
            return FPromiseHelper.<A>sequence(java.util.Arrays.asList(promises), HttpExecution.defaultContext());
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param ec Used to execute the sequencing operations.
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> sequence(ExecutionContext ec, Promise<A>... promises){
            return FPromiseHelper.<A>sequence(java.util.Arrays.asList(promises), ec);
        }

        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay (expressed with the corresponding unit).
         * @param unit The Unit.
         */
        public static <A> Promise<A> timeout(A message, long delay, TimeUnit unit) {
            return FPromiseHelper.timeout(message, delay, unit);
        }

        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay expressed in milliseconds.
         */
        public static <A> Promise<A> timeout(A message, long delay) {
            return timeout(message, delay, TimeUnit.MILLISECONDS);
        }

        /**
         * Create a Promise timer that throws a PromiseTimeoutException after
         * a given timeout.
         *
         * The returned Promise is usually combined with other Promises.
         *
         * @return a promise without a real value
         * @param delay The delay expressed in milliseconds.
         */
        public static Promise<scala.Unit> timeout(long delay) {
            return timeout(delay, TimeUnit.MILLISECONDS);
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
         */
        public static Promise<scala.Unit> timeout(long delay, TimeUnit unit) {
            return FPromiseHelper.timeout(delay, unit);
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * The sequencing operations are performed in the default ExecutionContext.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> sequence(Iterable<Promise<A>> promises){
            return FPromiseHelper.sequence(promises, HttpExecution.defaultContext());
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param promises The promises to combine
         * @param ec Used to execute the sequencing operations.
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> sequence(Iterable<Promise<A>> promises, ExecutionContext ec){
            return FPromiseHelper.sequence(promises, ec);
        }

        /**
         * Create a new pure promise, that is, a promise with a constant value from the start.
         *
         * @param a the value for the promise
         */
        public static <A> Promise<A> pure(final A a) {
            return FPromiseHelper.pure(a);
        }

        /**
         * Create a new promise throwing an exception.
         * @param throwable Value to throw
         */
        public static <A> Promise<A> throwing(Throwable throwable) {
            return FPromiseHelper.throwing(throwable);
        }

        /**
         * Create a Promise which will be redeemed with the result of a given function.
         *
         * The Function0 will be run in the default ExecutionContext.
         *
         * @param function Used to fulfill the Promise.
         */
        public static <A> Promise<A> promise(Supplier<A> function) {
            return FPromiseHelper.promise(function, HttpExecution.defaultContext());
        }

        /**
         * Create a Promise which will be redeemed with the result of a given Function0.
         *
         * @param function Used to fulfill the Promise.
         * @param ec The ExecutionContext to run the function in.
         */
        public static <A> Promise<A> promise(Supplier<A> function, ExecutionContext ec) {
            return FPromiseHelper.promise(function, ec);
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
         */
        public static <A> Promise<A> delayed(Supplier<A> function, long delay, TimeUnit unit) {
            return FPromiseHelper.delayed(function, delay, unit, HttpExecution.defaultContext());
        }

        /**
         * Create a Promise which, after a delay, will be redeemed with the result of a
         * given function. The function will be called after the delay.
         *
         * @param function The function to call to fulfill the Promise.
         * @param delay The time to wait.
         * @param unit The units to use for the delay.
         * @param ec The ExecutionContext to run the Function0 in.
         */
        public static <A> Promise<A> delayed(Supplier<A> function, long delay, TimeUnit unit, ExecutionContext ec) {
            return FPromiseHelper.delayed(function, delay, unit, ec);
        }

        /**
         * Awaits for the promise to get the result.<br>
         * Throws a Throwable if the calculation providing the promise threw an exception
         *
         * @param timeout A user defined timeout
         * @param unit timeout for timeout
         * @throws PromiseTimeoutException when the promise did timeout.
         * @return The promised result
         *
         */
        public A get(long timeout, TimeUnit unit) {
            return FPromiseHelper.get(this, timeout, unit);
        }

        /**
         * Awaits for the promise to get the result.<br>
         * Throws a Throwable if the calculation providing the promise threw an exception
         *
         * @param timeout A user defined timeout in milliseconds
         * @throws PromiseTimeoutException when the promise did timeout.
         * @return The promised result
         */
        public A get(long timeout) {
            return FPromiseHelper.get(this, timeout, TimeUnit.MILLISECONDS);
        }

        /**
         * Combines the current promise with <code>another</code> promise using `or`.
         *
         * @param another promise that will be combined
         */
        public <B> Promise<Either<A,B>> or(Promise<B> another) {
            return FPromiseHelper.or(this, another);
        }

        /**
         * Perform the given <code>action</code> callback when the Promise is redeemed.
         *
         * The callback will be run in the default execution context.
         *
         * @param action The action to perform.
         */
        public void onRedeem(final Consumer<A> action) {
            FPromiseHelper.onRedeem(this, action, HttpExecution.defaultContext());
        }

        /**
         * Perform the given <code>action</code> callback when the Promise is redeemed.
         *
         * @param action The action to perform.
         * @param ec The ExecutionContext to execute the action in.
         */
        public void onRedeem(final Consumer<A> action, ExecutionContext ec) {
            FPromiseHelper.onRedeem(this, action, ec);
        }

        /**
         * Maps this promise to a promise of type <code>B</code>.  The function <code>function</code> is applied as
         * soon as the promise is redeemed.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to map <code>A</code> to <code>B</code>.
         * @return A wrapped promise that maps the type from <code>A</code> to <code>B</code>.
         */
        public <B> Promise<B> map(final Function<? super A, B> function) {
            return FPromiseHelper.map(this, function, HttpExecution.defaultContext());
        }

        /**
         * Maps this promise to a promise of type <code>B</code>.  The function <code>function</code> is applied as
         * soon as the promise is redeemed.
         *
         * @param function The function to map <code>A</code> to <code>B</code>.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise that maps the type from <code>A</code> to <code>B</code>.
         */
        public <B> Promise<B> map(final Function<? super A, B> function, ExecutionContext ec) {
            return FPromiseHelper.map(this, function, ec);
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
         */
        public Promise<A> recover(final Function<Throwable,A> function) {
            return FPromiseHelper.recover(this, function, HttpExecution.defaultContext());
        }

        /**
         * Wraps this promise in a promise that will handle exceptions thrown by this Promise.
         *
         * @param function The function to handle the exception. This may, for example, convert the exception into something
         *      of type <code>T</code>, or it may throw another exception, or it may do some other handling.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise that will only throw an exception if the supplied <code>function</code> throws an
         *      exception.
         */
        public Promise<A> recover(final Function<Throwable,A> function, ExecutionContext ec) {
            return FPromiseHelper.recover(this, function, ec);
        }

        /**
         * Creates a new promise that will handle thrown exceptions by assigning to the value of another promise.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to handle the exception, and which returns another promise
         * @return A promise that will delegate to another promise on failure
         */
        public Promise<A> recoverWith(final Function<Throwable, Promise<A>> function) {
            return FPromiseHelper.recoverWith(this, function, HttpExecution.defaultContext());
        }

        /**
         * Creates a new promise that will handle thrown exceptions by assigning to the value of another promise.
         *
         * @param function The function to handle the exception, and which returns another promise
         * @param ec The ExecutionContext to execute the function in
         * @return A promise that will delegate to another promise on failure
         */
        public Promise<A> recoverWith(final Function<Throwable, Promise<A>> function, ExecutionContext ec) {
            return FPromiseHelper.recoverWith(this, function, ec);
        }

        /**
         * Creates a new promise which holds the result of this promise if it was completed successfully,
         * otherwise the result of the {@code fallback} promise if it completed successfully.
         * If both promises failed, the resulting promise holds the throwable of this promise.
         *
         * @param fallback The promise to fallback to if this promise has failed
         * @return A promise that will delegate to another promise on failure
         */
        public Promise<A> fallbackTo(final Promise<A> fallback) {
            return FPromiseHelper.fallbackTo(this, fallback);
        }

        /**
         * Perform the given <code>action</code> callback if the promise encounters an exception.
         *
         * This action will be run in the default exceution context.
         *
         * @param action The action to perform.
         */
        public void onFailure(final Consumer<Throwable> action) {
            FPromiseHelper.onFailure(this, action, HttpExecution.defaultContext());
        }

        /**
         * Perform the given <code>action</code> callback if the promise encounters an exception.
         *
         * @param action The action to perform.
         * @param ec The ExecutionContext to execute the callback in.
         */
        public void onFailure(final Consumer<Throwable> action, ExecutionContext ec) {
            FPromiseHelper.onFailure(this, action, ec);
        }

        /**
         * Maps the result of this promise to a promise for a result of type <code>B</code>, and flattens that to be
         * a single promise for <code>B</code>.
         *
         * The function will be run in the default execution context.
         *
         * @param function The function to map <code>A</code> to a promise for <code>B</code>.
         * @return A wrapped promise for a result of type <code>B</code>
         */
        public <B> Promise<B> flatMap(final Function<? super A,Promise<B>> function) {
            return FPromiseHelper.flatMap(this, function, HttpExecution.defaultContext());
        }

        /**
         * Maps the result of this promise to a promise for a result of type <code>B</code>, and flattens that to be
         * a single promise for <code>B</code>.
         *
         * @param function The function to map <code>A</code> to a promise for <code>B</code>.
         * @param ec The ExecutionContext to execute the function in.
         * @return A wrapped promise for a result of type <code>B</code>
         */
        public <B> Promise<B> flatMap(final Function<? super A,Promise<B>> function, ExecutionContext ec) {
            return FPromiseHelper.flatMap(this, function, ec);
        }

        /**
         * Creates a new promise by filtering the value of the current promise with a predicate.
         * If the predicate fails, the resulting promise will fail with a `NoSuchElementException`.
         *
         * @param predicate The predicate to test the current value.
         * @return A new promise with the current value, if the predicate is satisfied.
         */
        public Promise<A> filter(final Predicate<? super A> predicate) {
            return FPromiseHelper.filter(this, predicate, HttpExecution.defaultContext());
        }

        /**
         * Creates a new promise by filtering the value of the current promise with a predicate.
         * If the predicate fails, the resulting promise will fail with a `NoSuchElementException`.
         *
         * @param predicate The predicate to test the current value.
         * @param ec The ExecutionContext to execute the filtering in.
         * @return A new promise with the current value, if the predicate is satisfied.
         */
        public Promise<A> filter(final Predicate<? super A> predicate, ExecutionContext ec) {
            return FPromiseHelper.filter(this, predicate, ec);
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
         */
        public <B> Promise<B> transform(final Function<? super A, B> onSuccess, final Function<Throwable, Throwable> onFailure) {
            return FPromiseHelper.transform(this, onSuccess, onFailure, HttpExecution.defaultContext());
        }

        /**
         * Creates a new promise by applying the {@code onSuccess} function to a successful result,
         * or the {@code onFailure} function to a failed result.
         *
         * @param onSuccess The function to map a successful result from {@code A} to {@code B}
         * @param onFailure The function to map the {@code Throwable} when failed
         * @param ec The ExecutionContext to execute functions in
         * @return A new promise mapped by either the {@code onSuccess} or {@code onFailure} functions
         */
        public <B> Promise<B> transform(final Function<? super A, B> onSuccess, final Function<Throwable, Throwable> onFailure, ExecutionContext ec) {
            return FPromiseHelper.transform(this, onSuccess, onFailure, ec);
        }

        /**
         * Zips the values of this promise with <code>another</code>, and creates a new promise holding the tuple of their results
         * @param another
         */
        public <B> Promise<Tuple<A, B>> zip(Promise<B> another) {
            return wrap(wrapped().zip(another.wrapped())).map(
                new Function<scala.Tuple2<A, B>, Tuple<A, B>>() {
                    public Tuple<A, B> apply(scala.Tuple2<A, B> scalaTuple) {
                        return new Tuple(scalaTuple._1, scalaTuple._2);
                    }
                }
            );
        }

        /**
         * Gets the Scala Future wrapped by this Promise.
         *
         * @return The Scala Future
         */
        public Future<A> wrapped() {
            return future;
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
     */
    public static class RedeemablePromise<A> extends Promise<A>{

        private final scala.concurrent.Promise<A> promise;

        private RedeemablePromise(scala.concurrent.Promise<A> promise) {
            super(FPromiseHelper.getFuture(promise));

            this.promise = promise;
        }

        /**
         * Creates a new Promise with no value
         */
        public static <A> RedeemablePromise<A> empty() {
            scala.concurrent.Promise<A> p = FPromiseHelper.empty();
            return new RedeemablePromise(p);
        }

        /**
         * Completes the promise with a value.
         *
         * @param a The value to complete with
         */
        public void success(A a) {
            this.promise.success(a);
        }

        /**
         * Completes the promise with an exception
         *
         * @param t The exception to fail the promise with
         */
        public void failure(Throwable t) {
            this.promise.failure(t);
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be a null value,
         *         if the completion failed then the result will be an IllegalStateException.
         */
        public Promise<Void> completeWith(Promise other) {
            return this.completeWith(other, HttpExecution.defaultContext());
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
        public Promise<Void> completeWith(Promise other, ExecutionContext ec) {
            return Promise.wrap(FPromiseHelper.completeWith(this.promise, other.future, ec));
        }

        /**
         * Completes this promise with the specified Promise, once that Promise is completed.
         *
         * @param other The value to complete with
         * @return A promise giving the result of attempting to complete this promise with the other
         *         promise. If the completion was successful then the result will be true, if the
         *         completion couldn't occur then the result will be false.
         */
        public Promise<Boolean> tryCompleteWith(Promise other) {
            return this.tryCompleteWith(other, HttpExecution.defaultContext());
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
        public Promise<Boolean> tryCompleteWith(Promise other, ExecutionContext ec) {
            return Promise.wrap(FPromiseHelper.tryCompleteWith(this.promise, other.future, ec));
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

}
