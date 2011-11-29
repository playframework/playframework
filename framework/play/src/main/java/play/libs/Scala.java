package play.libs;

/*
 * class that contains useful java <-> scala conversion helpers
 */
public class Scala {

    public static <T> T orNull(scala.Option<T> opt) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return null;
    }

    public static <K,V> java.util.Map<K,V> asJava(scala.collection.Map<K,V> scalaMap) {
       return scala.collection.JavaConverters.asJavaMapConverter(scalaMap).asJava();
    }

    public static <T> java.util.List<T> asJava(scala.collection.immutable.List<T> scalaList) {
       return scala.collection.JavaConverters.asJavaListConverter(scalaList).asJava();
    }

    public static <T> scala.collection.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

    public static <T> scala.collection.Seq<T> toSeq(T[] array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    public static <T> scala.Option<T> Option(T t) {
        return scala.Option.apply(t);
    }

}
