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
 * Defines a set of functional helpers.
 */
public class F {

    /**
     * Represents optional values. Instances of Option are either an instance of Some or the object None.
     */
    public static abstract class Option<T> implements Iterable<T> {

        /**
         * is this value defined?
         */
        public abstract boolean isDefined();

        /**
         * Retrieve the value if defined.
         */
        public abstract T get();

        /**
         * Construct a None value.
         */
        public static None None() {
            return (None) new None();
        }

        /**
         * Construct a Some value.
         */
        public static <T> Some<T> Some(T value) {
            return new Some<T>(value);
        }
    }

    /**
     * Construct a None value.
     */
    public static <A> Some<A> Some(A a) {
        return new Some(a);
    }
    
    /**
     * Construct a None value.
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
     * Represents existing values of type T.
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
         * Construct a left side of the disjoint union, as opposed to the Right side.
         */
        public static <A, B> Either<A, B> Left(A value) {
            return new Either(Some(value), None());
        }

        /**
         * Construct a right side of the disjoint union, as opposed to the Left side.
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
     * A tuple of A,B
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
            return "T2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    /**
     * Construct a tuple of A,B
     */
    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple(a, b);
    }

    /**
     * A tuple of A,B
     */
    public static class T2<A, B> extends Tuple<A, B> {

        public T2(A _1, B _2) {
            super(_1, _2);
        }
    }

    /**
     * Construct a tuple of A,B
     */
    public static <A, B> T2<A, B> T2(A a, B b) {
        return new T2(a, b);
    }

    /**
     * A tuple of A,B,C
     */
    public static class T3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public T3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "T3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    /**
     * Construct a tuple of A,B,C
     */
    public static <A, B, C> T3<A, B, C> T3(A a, B b, C c) {
        return new T3(a, b, c);
    }

    /**
     * A tuple of A,B,C,D
     */
    public static class T4<A, B, C, D> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;

        public T4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "T4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    /**
     * Construct a tuple of A,B,C,D
     */
    public static <A, B, C, D> T4<A, B, C, D> T4(A a, B b, C c, D d) {
        return new T4<A, B, C, D>(a, b, c, d);
    }

    /**
     * A tuple of A,B,C,D,E
     */
    public static class T5<A, B, C, D, E> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;

        public T5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "T5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }
    }

    /**
     * Construct a tuple of A,B,C,D,E
     */
    public static <A, B, C, D, E> T5<A, B, C, D, E> T5(A a, B b, C c, D d, E e) {
        return new T5<A, B, C, D, E>(a, b, c, d, e);
    }

}
