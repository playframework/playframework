/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

class CrossScala {
    /**
     * Converts a Java List to Scala Seq.
     *
     * @param list    the java list.
     * @return the converted Seq.
     * @param <T> the element type.
     */
    public static <T> scala.collection.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala();
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
     * @return the Scala varargs
     * @param <T> the element type.
     */
    @SafeVarargs
    public static <T> scala.collection.Seq<T> varargs(T... array) {
        return toSeq(array);
    }
}
