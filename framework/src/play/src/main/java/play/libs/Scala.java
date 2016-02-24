/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import akka.japi.JavaPartialFunction;
import scala.runtime.AbstractFunction0;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Class that contains useful java &lt;-&gt; scala conversion helpers.
 */
public class Scala {

    /**
     * Wrap a Scala Option, handling None as null.
     */
    public static <T> T orNull(scala.Option<T> opt) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return null;
    }

    /**
     * Wrap a Scala Option, handling None by returning a defaultValue
     */
    public static <T> T orElse(scala.Option<T> opt, T defaultValue) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return defaultValue;
    }

    /**
     * Converts a Scala Map to Java.
     */
    public static <K,V> java.util.Map<K,V> asJava(scala.collection.Map<K,V> scalaMap) {
        return scala.collection.JavaConverters.mapAsJavaMapConverter(scalaMap).asJava();
    }

    /**
     * Converts a Java Map to Scala.
     */
    public static <A,B> scala.collection.immutable.Map<A,B> asScala(Map<A,B> javaMap) {
        return play.utils.Conversions.newMap(
                scala.collection.JavaConverters.mapAsScalaMapConverter(javaMap).asScala().toSeq()
                );
    }

    /**
     * Converts a Java Collection to a Scala Seq.
     */
    public static <A> scala.collection.immutable.Seq<A> asScala(Collection<A> javaCollection) {
        return scala.collection.JavaConverters.collectionAsScalaIterableConverter(javaCollection).asScala().toList();
    }

    /**
     * Converts a Java Callable to a Scala Function0.
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
     * Converts a Scala List to Java.
     */
    public static <T> java.util.List<T> asJava(scala.collection.Seq<T> scalaList) {
        return scala.collection.JavaConverters.seqAsJavaListConverter(scalaList).asJava();
    }

    /**
     * Converts a Scala List to an Array.
     */
    public static <T> T[] asArray(Class<T> clazz, scala.collection.Seq<T> scalaList) {
        T[] arr = (T[]) Array.newInstance(clazz, scalaList.length());
        scalaList.copyToArray(arr);
        return arr;
    }

    /**
     * Converts a Java List to Scala Seq.
     */
    public static <T> scala.collection.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

    /**
     * Converts a Java Array to Scala Seq.
     */
    public static <T> scala.collection.Seq<T> toSeq(T[] array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    /**
     * Converts a Java varargs to Scala Seq.
     */
    public static <T> scala.collection.Seq<T> varargs(T... array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    /**
     * Wrap a value into a Scala Option.
     */
    public static <T> scala.Option<T> Option(T t) {
        return scala.Option.apply(t);
    }

    /**
     * None
     */
    public static <T> scala.Option<T> None() {
        return (scala.Option<T>) scala.None$.MODULE$;
    }

    /**
     * Create a Scala Tuple2.
     */
    @SuppressWarnings("unchecked")
    public static <A,B> scala.Tuple2<A,B> Tuple(A a, B b) {
        return new scala.Tuple2<A, B>(a, b);
    }

    /**
     *  Convert a scala Tuple2 to a java F.Tuple.
     */
    public static <A, B> F.Tuple<A, B> asJava(scala.Tuple2<A, B> tuple) {
        return F.Tuple(tuple._1(), tuple._2());
    }

    /**
     * Creates an empty Scala Seq.
     */
    @SuppressWarnings("unchecked")
    public static <T> scala.collection.Seq<T> emptySeq() {
        return (scala.collection.Seq<T>)toSeq(new Object[] {});
    }

    /**
     * Creates an empty Scala Map.
     */
    public static <A,B> scala.collection.immutable.Map<A,B> emptyMap() {
        return new scala.collection.immutable.HashMap<A,B>();
    }

    /**
     * Returns an any ClassTag typed according to the Java compiler as C.
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
     * @param f The function to make a partial function from.
     * @return A Scala PartialFunction.
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
