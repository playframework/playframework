package play.i18n;

import scala.collection.mutable.Buffer;

import java.util.Arrays;

/**
 * High-level internationalisation API.
 */
public class Messages {

    /**
    * Translates a message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param lang the message lang
    * @param key the message key
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
    */
    public static String get(Lang lang, String key, Object... args) {
        Buffer<Object> scalaArgs = scala.collection.JavaConverters.asScalaBufferConverter(Arrays.asList(args)).asScala();
        return play.api.i18n.Messages.apply(key, scalaArgs, lang);
    }
    
    /**
    * Translates a message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param key the message key
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
    */
    public static String get(String key, Object... args) {
        Buffer<Object> scalaArgs = scala.collection.JavaConverters.asScalaBufferConverter(Arrays.asList(args)).asScala();
        return play.api.i18n.Messages.apply(key, scalaArgs, play.mvc.Http.Context.Implicit.lang());
    }
    
}
