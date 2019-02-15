/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import scala.concurrent.ExecutionContext;

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
     * A Function with 4 arguments.
     */
    public interface Function4<A,B,C,D,R> {
        R apply(A a, B b, C c, D d) throws Throwable;
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
         * @param <L> the left type
         * @param <R> the right type
         * @return A left sided disjoint union
         */
        public static <L, R> Either<L, R> Left(L value) {
            return new Either<L, R>(Optional.of(value), Optional.<R>empty());
        }

        /**
         * Constructs a right side of the disjoint union, as opposed to the Left side.
         *
         * @param value The value of the right side
         * @param <L> the left type
         * @param <R> the right type
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
     * @param <A> a's type
     * @param <B> b's type
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
     * @param <A> a's type
     * @param <B> b's type
     * @param <C> c's type
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
     * @param <A> a's type
     * @param <B> b's type
     * @param <C> c's type
     * @param <D> d's type
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
     * @param <A> a's type
     * @param <B> b's type
     * @param <C> c's type
     * @param <D> d's type
     * @param <E> e's type
     * @return The tuple
     */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> Tuple5(A a, B b, C c, D d, E e) {
        return new Tuple5<A, B, C, D, E>(a, b, c, d, e);
    }

    /**
     * Converts the execution context to an executor, preparing it first.
     * @param ec    the execution context.
     * @return      the Java Executor.
     */
    private static Executor toExecutor(ExecutionContext ec) {
        ExecutionContext prepared = ec.prepare();
        if (prepared instanceof Executor) {
            return (Executor) prepared;
        } else {
            return prepared::execute;
        }
    }

    public static class LazySupplier<T> implements Supplier<T> {

        private T value;

        private final Supplier<T> instantiator;

        private LazySupplier(Supplier<T> instantiator) {
            this.instantiator = instantiator;
        }

        @Override
        public T get() {
            if (this.value == null) {
                this.value = instantiator.get();
            }
            return this.value;
        }

        public static <T> Supplier<T> lazy(Supplier<T> creator) {
            return new LazySupplier<>(creator);
        }
    }

}
