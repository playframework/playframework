package play.libs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Defines a set of functional programming style helpers.
 */
public class F {

    /**
     * A Callback with no arguments.
     */
    public static interface Callback0 {
        public void invoke() throws Throwable;
    }

    /**
     * A Callback with a single argument.
     */
    public static interface Callback<A> {
        public void invoke(A a) throws Throwable;
    }

    /**
     * A Callback with 2 arguments.
     */
    public static interface Callback2<A,B> {
        public void invoke(A a, B b) throws Throwable;
    }

    /**
     * A Callback with 3 arguments.
     */
    public static interface Callback3<A,B,C> {
        public void invoke(A a, B b, C c) throws Throwable;
    }
    
    /**
     * A Function with no arguments.
     */
    public static interface Function0<R> {
        public R apply() throws Throwable;
    }

    /**
     * A Function with a single argument.
     */
    public static interface Function<A,R> {
        public R apply(A a) throws Throwable;
    }

    /**
     * A Function with 2 arguments.
     */
    public static interface Function2<A,B,R> {
        public R apply(A a, B b) throws Throwable;
    }

    /**
     * A Function with 3 arguments.
     */
    public static interface Function3<A,B,C,R> {
        public R apply(A a, B b, C c) throws Throwable;
    }

    public static class Promise<A> {

        private final play.api.libs.concurrent.Promise<A> promise;

        public Promise(play.api.libs.concurrent.Promise<A> promise) {
            this.promise = promise;
        }

        public A get() {
            return promise.value().get();
        }

        public void onRedeem(final Callback<A> action) {
            promise.onRedeem(new scala.runtime.AbstractFunction1<A,scala.runtime.BoxedUnit>() {
                public scala.runtime.BoxedUnit apply(A a) {
                    try {
                        action.invoke(a);
                    } catch(Throwable t) {
                        throw new RuntimeException();
                    }
                    return null;
                }
            });
        }

        public <B> Promise<B> map(final Function<A,B> f) {
            return new Promise(
                promise.map(new scala.runtime.AbstractFunction1<A,B>() {
                    public B apply(A a) {
                        try {
                            return f.apply(a);
                        } catch(Throwable t) {
                            throw new RuntimeException();
                        }
                    }
                })
            );
        }

        public <B> Promise<B> flatMap(final Function<A,Promise<B>> f) {
            return new Promise(
                promise.flatMap(new scala.runtime.AbstractFunction1<A,play.api.libs.concurrent.Promise<B>>() {
                    public play.api.libs.concurrent.Promise<B> apply(A a) {
                        try {
                            return f.apply(a).promise;
                        } catch(Throwable t) {
                            throw new RuntimeException();
                        }
                    }
                })
            );
        }

        public play.api.libs.concurrent.Promise<A> getWrappedPromise() {
            return promise;
        }

    }

    /**
     * Represents optional values. Instances of <code>Option</code> are either an instance of <code>Some</code> or the object <code>None</code>.
     */
    public static abstract class Option<T> implements Iterable<T> {

        /**
         * Returns <code>true</code> if this value is defined.
         */
        public abstract boolean isDefined();

        /**
         * Returns the value if defined.
         */
        public abstract T get();

        /**
         * Constructs a <code>None</code> value.
         */
        public static <T> None<T> None() {
            return (None) new None();
        }

        /**
         * Construct a <code>Some</code> value.
         */
        public static <T> Some<T> Some(T value) {
            return new Some<T>(value);
        }
        
        public T getOrElse(T defaultValue) {
            if(isDefined()) {
                return get();
            } else {
                return defaultValue;
            }
        }
        
        public <A> Option<A> map(Function<T,A> f) {
            if(isDefined()) {
                try {
                    return Some(f.apply(get()));
                } catch(Throwable t) {
                    throw new RuntimeException();
                }
            } else {
                return None();
            }
        }
        
    }

    /**
     * Construct a <code>Some</code> value.
     */
    public static <A> Some<A> Some(A a) {
        return new Some(a);
    }

    /**
     * Constructs a <code>None</code> value.
     */
    public static None None() {
        return new None();
    }

    /**
     * Represents non-existent values.
     */
    public static class None<T> extends Option<T> {

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("No value");
        }

        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    /**
     * Represents existing values of type <code>T</code>.
     */
    public static class Some<T> extends Option<T> {

        final T value;

        public Some(T value) {
            this.value = value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    /**
     * Represents a value of one of two possible types (a disjoint union)
     */
    public static class Either<A, B> {

        /**
         * The left value.
         */
        final public Option<A> left;

        /**
         * The right value.
         */
        final public Option<B> right;

        private Either(Option<A> left, Option<B> right) {
            this.left = left;
            this.right = right;
        }

        /**
         * Constructs a left side of the disjoint union, as opposed to the Right side.
         */
        public static <A, B> Either<A, B> Left(A value) {
            return new Either(Some(value), None());
        }

        /**
         * Constructs a right side of the disjoint union, as opposed to the Left side.
         */
        public static <A, B> Either<A, B> Right(B value) {
            return new Either(None(), Some(value));
        }

        @Override
        public String toString() {
            return "Either(left: " + left + ", right: " + right + ")";
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
    }

    /**
     * Constructs a tuple of A,B
     */
    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple(a, b);
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
    }

    /**
     * Constructs a tuple of A,B,C
     */
    public static <A, B, C> Tuple3<A, B, C> Tuple3(A a, B b, C c) {
        return new Tuple3(a, b, c);
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
    }

    /**
     * Constructs a tuple of A,B,C,D
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
    }

    /**
     * Constructs a tuple of A,B,C,D,E
     */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> Tuple5(A a, B b, C c, D d, E e) {
        return new Tuple5<A, B, C, D, E>(a, b, c, d, e);
    }

}
