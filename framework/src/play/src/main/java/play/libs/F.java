package play.libs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    
    /**
     * A promise to produce a result of type <code>A</code>.
     */
    public static class Promise<A> {

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> waitAll(Promise<? extends A>... promises){
            return new Promise<List<A>>(play.core.j.JavaPromise.<A>sequence(java.util.Arrays.asList(promises)));
        }
        
        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay (expressed with the corresponding unit).
         * @param unit The Unit.
         */
        public static <A> Promise<A> timeout(A message, Long delay, java.util.concurrent.TimeUnit unit) {
            return new Promise(play.core.j.JavaPromise.timeout(message, delay, unit));
        }
        
       /**
        * Create a Promise that is redeemed after the default timeout expires.
        *
        */
        public static Promise<TimeoutException> timeout() {
            return new Promise(play.core.j.JavaPromise.timeout());
        }

        /**
         * Create a Promise that is redeemed after a timeout.
         *
         * @param message The message to use to redeem the Promise.
         * @param delay The delay expressed in Milliseconds.
         */
        public static <A> Promise<A> timeout(A message, Long delay) {
            return timeout(message, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        /**
         * Combine the given promises into a single promise for the list of results.
         *
         * @param promises The promises to combine
         * @return A single promise whose methods act on the list of redeemed promises
         */
        public static <A> Promise<List<A>> waitAll(Iterable<Promise<? extends A>> promises){
            ArrayList<Promise<? extends A>> ps = new ArrayList<Promise<? extends A>>();
            for(Promise<? extends A> p : promises){
                ps.add(p);
            }
            return new Promise<List<A>>(play.core.j.JavaPromise.<A>sequence(ps));
        }

        /**
         * Create a new pure promise, that is, a promise with a constant value from the start.
         *
         * @param a the value for the promise
         */
        public static <A> Promise<A> pure(final A a) {
            return new Promise<A>(play.core.j.JavaPromise.<A>pure(a));
        }

        /**
         * Create a new promise throwing an exception.
         * @param throwable Value to throw
         */
        public static <A> Promise<A> throwing(Throwable throwable) {
            return new Promise<A>(play.core.j.JavaPromise.<A>throwing(throwable));
        }

        private final play.api.libs.concurrent.Promise<A> promise;

        /**
         * Create a new promise wrapping the given Scala promise
         *
         * @param promise The scala promise to wrap
         */
        public Promise(play.api.libs.concurrent.Promise<A> promise) {
            this.promise = promise;
        }

        /**
         * Awaits for the promise to get the result using the default timeout (5000 milliseconds).
         *
         * @return The promised object
         * @throws RuntimeException if the calculation providing the promise threw an exception
         */
        public A get() {
            return promise.value().get();
        }
        
        /**
         * Awaits for the promise to get the result.
         *
         * @param timeout A user defined timeout
         * @param unit timeout for timeout
         * @return The promised result
         * @throws RuntimeException if the calculation providing the promise threw an exception
         */
        public A get(Long timeout, TimeUnit unit) {
            return promise.await(timeout, unit).get();
        }

        /**
         * Awaits for the promise to get the result.
         *
         * @param timeout A user defined timeout in milliseconds
         * @return The promised result
         * @throws RuntimeException if the calculation providing the promise threw an exception
         */
        public A get(Long timeout) {
            return get(timeout, TimeUnit.MILLISECONDS);
        }

       /**
        * combines the current promise with <code>another</code> promise using or
        * @param another 
        */
        public <B> Promise<Either<A,B>> or(Promise<B> another) {
            return (new Promise(this.promise.or(another.getWrappedPromise()))).map(
                 new Function<scala.Either<A,B>,Either<A,B>>() {
                    public Either<A,B> apply(scala.Either<A,B> scalaEither) {
                        if (scalaEither.left().toOption().isDefined() == true) 
                            return Either.Left(scalaEither.left().get());
                        else 
                            return Either.Right(scalaEither.right().get());
                    }
                 }
                );
        }
        /**
         * Perform the given <code>action</code> callback when the Promise is redeemed.
         *
         * @param action The action to perform.
         */
        public void onRedeem(final Callback<A> action) {
            final play.mvc.Http.Context context = play.mvc.Http.Context.current.get();
            promise.onRedeem(new scala.runtime.AbstractFunction1<A,scala.runtime.BoxedUnit>() {
                public scala.runtime.BoxedUnit apply(A a) {
                    try {
                        run(new Function<A,Object>() {
                            public Object apply(A a) {
                                try {
                                    action.invoke(a);
                                    return 0;
                                } catch(RuntimeException e) {
                                    throw e;
                                } catch(Throwable t) {
                                    throw new RuntimeException(t);
                                }
                            }
                        }, a, context);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    return null;
                }
            });
        }

        /**
         * Maps this promise to a promise of type <code>B</code>.  The function <code>function</code> is applied as
         * soon as the promise is redeemed.
         *
         * Exceptions thrown by <code>function</code> will be wrapped in {@link java.lang.RuntimeException}, unless
         * they are <code>RuntimeException</code>'s themselves.
         *
         * @param function The function to map <code>A</code> to <code>B</code>.
         * @return A wrapped promise that maps the type from <code>A</code> to <code>B</code>.
         */
        public <B> Promise<B> map(final Function<A, B> function) {
            final play.mvc.Http.Context context = play.mvc.Http.Context.current.get();
            return new Promise<B>(
                promise.flatMap(new scala.runtime.AbstractFunction1<A,play.api.libs.concurrent.Promise<B>>() {
                    public play.api.libs.concurrent.Promise<B> apply(A a) {
                        try {
                            return run(function, a, context);
                        } catch (RuntimeException e) {
                            throw e;
                        } catch(Throwable t) {
                            throw new RuntimeException(t);
                        }
                    }
                })
            );
        }

        /**
         * Wraps this promise in a promise that will handle exceptions thrown by this Promise.
         *
         * Exceptions thrown by <code>function</code> will be wrapped in {@link java.lang.RuntimeException}, unless
         * they are <code>RuntimeException</code>'s themselves.
         *
         * @param function The function to handle the exception. This may, for example, convert the exception into something
         *      of type <code>T</code>, or it may throw another exception, or it may do some other handling.
         * @return A wrapped promise that will only throw an exception if the supplied <code>function</code> throws an
         *      exception.
         */
        public Promise<A> recover(final Function<Throwable,A> function) {
            final play.mvc.Http.Context context = play.mvc.Http.Context.current.get();
            return new Promise<A>(
                play.core.j.JavaPromise.recover(promise, new scala.runtime.AbstractFunction1<Throwable, play.api.libs.concurrent.Promise<A>>() {
                    public play.api.libs.concurrent.Promise<A> apply(Throwable t) {
                        try {
                            return run(function,t, context);
                        } catch (RuntimeException e) {
                            throw e;
                        } catch(Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
            );
        }

        /**
         * Maps the result of this promise to a promise for a result of type <code>B</code>, and flattens that to be
         * a single promise for <code>B</code>.
         *
         * Exceptions thrown by <code>function</code> will be wrapped in {@link java.lang.RuntimeException}, unless
         * they are <code>RuntimeException</code>'s themselves.
         *
         * @param function The function to map <code>A</code> to a promise for <code>B</code>.
         * @return A wrapped promise for a result of type <code>B</code>
         */
        public <B> Promise<B> flatMap(final Function<A,Promise<B>> function) {
            final play.mvc.Http.Context context = play.mvc.Http.Context.current.get();
            return new Promise<B>(
                promise.flatMap(new scala.runtime.AbstractFunction1<A,play.api.libs.concurrent.Promise<Promise<B>>>() {
                    public play.api.libs.concurrent.Promise<Promise<B>> apply(A a) {
                        try {
                            return run(function, a, context);
                        } catch (RuntimeException e) {
                            throw e;
                        } catch(Throwable t) {
                            throw new RuntimeException(t);
                        }
                    }
                }).flatMap(new scala.runtime.AbstractFunction1<Promise<B>,play.api.libs.concurrent.Promise<B>>() {
                    public play.api.libs.concurrent.Promise<B> apply(Promise<B> p) {
                        return p.promise;
                    }
                })
            );
        }

        /**
         * Get the underlying Scala promise
         *
         * @return The scala promise
         */
        public play.api.libs.concurrent.Promise<A> getWrappedPromise() {
            return promise;
        }
        
        // -- Utils

        static Integer nb = 64;

        static List<akka.actor.ActorRef> actors = null;
        static List<akka.actor.ActorRef> actors() {
            synchronized(Promise.class) {
                if(actors == null) {
                    synchronized(Promise.class) {
                        actors = new ArrayList<akka.actor.ActorRef>(nb);
                        for(int i=0; i<nb; i++) {
                            actors.add(play.api.libs.concurrent.Promise$.MODULE$.system().actorOf(new akka.actor.Props(PromiseActor.class), "promise-actor-" + i));
                        }
                    }
                }
            }
            return actors;
        }

        static <A,B> play.api.libs.concurrent.Promise<B> run(Function<A,B> f, A a, play.mvc.Http.Context context) {
            Long id;
            if(context == null) {
                id = 0l;
            } else {
                id = context.id();
            }
            return new play.api.libs.concurrent.AkkaPromise(
                (akka.dispatch.Future<Object>)akka.pattern.Patterns.ask(
                    actors().get((int)(id % actors().size())), 
                    Tuple3(f, a, context), 
                    akka.util.Timeout.apply(60000 * 60 * 1) // Let's wait 1h here. Unfortunately we can't avoid a timeout.
                )
            ).map(new scala.runtime.AbstractFunction1<Object,B> () {
                public B apply(Object o) {
                    Either<Throwable,B> r = (Either<Throwable,B>)o;
                    if(r.left.isDefined()) {
                        Throwable t = r.left.get();
                        if(t instanceof RuntimeException) {
                            throw (RuntimeException)t;
                        } else {
                            throw new RuntimeException(t);
                        }
                    }
                    return r.right.get();
                }
            });
        }

        // Executes the Promise functions (capturing exception), with the given ThreadLocal context.
        // This Actor is used as Agent to ensure function execution ordering for a given context.
        public static class PromiseActor extends akka.actor.UntypedActor {

            public void onReceive(Object o) {
                Function f = (Function)(((Tuple3)o)._1);
                Object a = (Object)(((Tuple3)o)._2);
                play.mvc.Http.Context context = (play.mvc.Http.Context)(((Tuple3)o)._3);
                try {
                    play.mvc.Http.Context.current.set(context);
                    getSender().tell(Either.Right(f.apply(a)));
                } catch(Throwable t) {
                    getSender().tell(Either.Left(t));
                } finally {
                    play.mvc.Http.Context.current.remove();
                }
            }

        }

    }

    /**
     * Represents optional values. Instances of <code>Option</code> are either an instance of <code>Some</code> or the object <code>None</code>.
     */
    public static abstract class Option<T> implements Iterable<T> {

        /**
         * Is the value of this option defined?
         *
         * @return <code>true</code> if the value is defined, otherwise <code>false</code>.
         */
        public abstract boolean isDefined();

        /**
         * Returns the value if defined.
         *
         * @return The value if defined, otherwise <code>null</code>.
         */
        public abstract T get();

        /**
         * Constructs a <code>None</code> value.
         *
         * @return None
         */
        public static <T> None<T> None() {
            return new None<T>();
        }

        /**
         * Construct a <code>Some</code> value.
         *
         * @param value The value to make optional
         * @return Some <code>T</code>.
         */
        public static <T> Some<T> Some(T value) {
            return new Some<T>(value);
        }

        /**
         * Get the value if defined, otherwise return the supplied <code>defaultValue</code>.
         *
         * @param defaultValue The value to return if the value of this option is not defined
         * @return The value of this option, or <code>defaultValue</code>.
         */
        public T getOrElse(T defaultValue) {
            if(isDefined()) {
                return get();
            } else {
                return defaultValue;
            }
        }

        /**
         * Map this option to another value.
         *
         * @param function The function to map the option using.
         * @return The mapped option.
         * @throws RuntimeException if <code>function</code> threw an Exception.  If the exception is a
         *      <code>RuntimeException</code>, it will be rethrown as is, otherwise it will be wrapped in a
         *      <code>RuntimeException</code>.
         */
        public <A> Option<A> map(Function<T,A> function) {
            if(isDefined()) {
                try {
                    return Some(function.apply(get()));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else {
                return None();
            }
        }
        
    }

    /**
     * Construct a <code>Some</code> value.
     *
     * @param value The value
     * @return Some value.
     */
    public static <A> Some<A> Some(A value) {
        return new Some<A>(value);
    }

    /**
     * Constructs a <code>None</code> value.
     *
     * @return None.
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
         *
         * @param value The value of the left side
         * @return A left sided disjoint union
         */
        public static <A, B> Either<A, B> Left(A value) {
            return new Either<A, B>(Some(value), None());
        }

        /**
         * Constructs a right side of the disjoint union, as opposed to the Left side.
         *
         * @param value The value of the right side
         * @return A right sided disjoint union
         */
        public static <A, B> Either<A, B> Right(B value) {
            return new Either<A, B>(None(), Some(value));
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
