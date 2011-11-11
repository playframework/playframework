package play.libs;

public class Scala {
    
    public static <T> T orNull(scala.Option<T> opt) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return null;
    }
    
    public static <T> scala.collection.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }
    
    public static <T> scala.collection.Seq<T> toSeq(T[] array) {
        return toSeq(java.util.Arrays.asList(array));
    }
    
}