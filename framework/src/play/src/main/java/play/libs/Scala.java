/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import akka.japi.JavaPartialFunction;
import scala.compat.java8.FutureConverters;
import scala.runtime.AbstractFunction0;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Class that contains useful java &lt;-&gt; scala conversion helpers.
 */
public class Scala {

    /**
     * Wraps a Scala Option, handling None as null.
     *
     * @param opt the scala option.
     * @param <T> the type in the Option.
     * @return the value of the option, or null if opt.isDefined is false.
     */
    public static <T> T orNull(scala.Option<T> opt) {
        if (opt.isDefined()) {
            return opt.get();
        }
        return null;
    }

    /**
     * Wraps a Scala Option, handling None by returning a defaultValue
     *
     * @param opt          the scala option.
     * @param defaultValue the default value if None is found.
     * @param <T>          the type in the Option.
     * @return the return value.
     */
    public static <T> T orElse(scala.Option<T> opt, T defaultValue) {
        if (opt.isDefined()) {
            return opt.get();
        }
        return defaultValue;
    }

    /**
     * Converts a Scala Map to Java.
     *
     * @param scalaMap the scala map.
     * @param <K>      key type
     * @param <V>      value type
     * @return the java map.
     */
    public static <K, V> java.util.Map<K, V> asJava(scala.collection.Map<K, V> scalaMap) {
        return scala.collection.JavaConverters.mapAsJavaMapConverter(scalaMap).asJava();
    }

    /**
     * Converts a Java Map to Scala.
     *
     * @param javaMap the java map
     * @param <K>     key type
     * @param <V>     value type
     * @return the scala map.
     */
    public static <K, V> scala.collection.immutable.Map<K, V> asScala(Map<K, V> javaMap) {
        return play.utils.Conversions.newMap(
                scala.collection.JavaConverters.mapAsScalaMapConverter(javaMap).asScala().toSeq()
        );
    }

    /**
     * Converts a Java Collection to a Scala Seq.
     *
     * @param javaCollection the java collection
     * @param <A>            the type of Seq element
     * @return the scala Seq.
     */
    public static <A> scala.collection.immutable.Seq<A> asScala(Collection<A> javaCollection) {
        return scala.collection.JavaConverters.collectionAsScalaIterableConverter(javaCollection).asScala().toList();
    }

    /**
     * Converts a Java Callable to a Scala Function0.
     *
     * @param callable the java callable.
     * @param <A>      the return type.
     * @return the scala function.
     */
    public static <A> scala.Function0<A> asScala(final Callable<A> callable) {
        return new AbstractFunction0<A>() {
            @Override
            public A apply() {
                try {
                    return callable.call();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };
    }

    /**
     * Converts a Java Callable to a Scala Function0.
     *
     * @param callable the java callable.
     * @param <A>      the return type.
     * @return the scala function in a Scala Future.
     */
    public static <A> scala.Function0<scala.concurrent.Future<A>> asScalaWithFuture(final Callable<CompletionStage<A>> callable) {
        return new AbstractFunction0<scala.concurrent.Future<A>>() {
            @Override
            public scala.concurrent.Future<A> apply() {
                try {
                    return FutureConverters.toScala(callable.call());
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };
    }

    /**
     * Converts a Scala List to Java.
     *
     * @param scalaList    the scala list.
     * @return the java list
     * @param <T> the return type.
     */
    public static <T> java.util.List<T> asJava(scala.collection.Seq<T> scalaList) {
        return scala.collection.JavaConverters.seqAsJavaListConverter(scalaList).asJava();
    }

    /**
     * Converts a Scala List to an Array.
     *
     * @param clazz    the element class type
     * @param scalaList the scala list.
     * @param <T> the return type.
     * @return the array
     */
    public static <T> T[] asArray(Class<T> clazz, scala.collection.Seq<T> scalaList) {
        T[] arr = (T[]) Array.newInstance(clazz, scalaList.length());
        scalaList.copyToArray(arr);
        return arr;
    }

    /**
     * Converts a Java List to Scala Seq.
     *
     * @param list    the java list.
     * @return the converted Seq.
     * @param <T> the element type.
     */
    public static <T> scala.collection.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

    /**
     * Converts a Java Array to Scala Seq.
     *
     * @param array    the java array.
     * @return the converted Seq.
     * @param <T> the element type.
     */
    public static <T> scala.collection.Seq<T> toSeq(T[] array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    /**
     * Converts a Java varargs to Scala Seq.
     *
     * @param array    the java array.
     * @return the converted Seq.
     * @param <T> the element type.
     */
    @SafeVarargs
    public static <T> scala.collection.Seq<T> varargs(T... array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    /**
     * Wrap a value into a Scala Option.
     *
     * @param t    the java value.
     * @return the converted Option.
     * @param <T> the element type.
     */
    public static <T> scala.Option<T> Option(T t) {
        return scala.Option.apply(t);
    }

    /**
     * @param <T> the type parameter
     * @return a scala {@code None}.
     */
    public static <T> scala.Option<T> None() {
        return (scala.Option<T>) scala.None$.MODULE$;
    }

    /**
     * Creates a Scala {@code Tuple2}.
     *
     * @param a element one of the tuple.
     * @param b element two of the tuple.
     * @param <A> input parameter type
     * @param <B> return type.
     * @return an instance of Tuple2 with the elements.
     */
    @SuppressWarnings("unchecked")
    public static <A, B> scala.Tuple2<A, B> Tuple(A a, B b) {
        return new scala.Tuple2<A, B>(a, b);
    }

    /**
     * Converts a scala {@code Tuple2} to a java F.Tuple.
     *
     * @param tuple the Scala Tuple.
     * @param <A> input parameter type
     * @param <B> return type.
     * @return an instance of Tuple with the elements.
     */
    public static <A, B> F.Tuple<A, B> asJava(scala.Tuple2<A, B> tuple) {
        return F.Tuple(tuple._1(), tuple._2());
    }

    /**
     * @param <T> the type parameter
     * @return an empty Scala Seq.
     */
    @SuppressWarnings("unchecked")
    public static <T> scala.collection.Seq<T> emptySeq() {
        return (scala.collection.Seq<T>) toSeq(new Object[]{});
    }

    /**
     * @return an empty Scala Map.
     * @param <A> input parameter type
     * @param <B> return type.
     */
    public static <A, B> scala.collection.immutable.Map<A, B> emptyMap() {
        return new scala.collection.immutable.HashMap<A, B>();
    }

    /**
     * @param <C> the classtag's type.
     * @return an any ClassTag typed according to the Java compiler as C.
     */
    public static <C> scala.reflect.ClassTag<C> classTag() {
        return (scala.reflect.ClassTag<C>) scala.reflect.ClassTag$.MODULE$.Any();
    }


    /**
     * Create a Scala PartialFunction from a function.
     *
     * A PartialFunction is one that isn't defined for the whole of its domain. If the function isn't defined for a
     * particular input parameter, it can throw <code>F.noMatch()</code>, and this will be translated into the semantics
     * of a Scala PartialFunction.
     *
     * For example:
     *
     * <pre>
     *     Flow&lt;String, Integer, ?&gt; collectInts = Flow.&lt;String&gt;collect(Scala.partialFunction( str -&gt; {
     *         try {
     *             return Integer.parseInt(str);
     *         } catch (NumberFormatException e) {
     *             throw Scala.noMatch();
     *         }
     *     }));
     * </pre>
     *
     * The above code will convert a flow of String into a flow of Integer, dropping any strings that can't be parsed
     * as integers.
     *
     * @param f   The function to make a partial function from.
     * @param <A> input parameter type
     * @param <B> return type.
     * @return a Scala PartialFunction.
     */
    public static <A, B> scala.PartialFunction<A, B> partialFunction(Function<A, B> f) {
        return new JavaPartialFunction<A, B>() {
            @Override
            public B apply(A a, boolean isCheck) throws Exception {
                return f.apply(a);
            }
        };
    }

    /**
     * Throw this exception to indicate that a partial function doesn't match.
     *
     * @return An exception that indicates a partial function doesn't match.
     */
    public static RuntimeException noMatch() {
        return JavaPartialFunction.noMatch();
    }

}
