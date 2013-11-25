package play.libs;

import java.util.*;

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
     * Converts a Scala List to Java.
     */
    public static <T> java.util.List<T> asJava(scala.collection.Seq<T> scalaList) {
        return scala.collection.JavaConverters.asJavaListConverter(scalaList).asJava();
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
        return scala.Option.apply(null);
    }

    /**
     * Create a Scala Tuple2.
     */
    @SuppressWarnings("unchecked")
    public static <A,B> scala.Tuple2<A,B> Tuple(A a, B b) {
        return new scala.Tuple2<A, B>(a, b);
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

}
