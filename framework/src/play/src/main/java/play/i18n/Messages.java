package play.i18n;

import scala.collection.mutable.Buffer;

import java.util.Arrays;

/**
 * High-level internationalisation API.
 *
 * For example:
 * {{{
 * String msgString = Messages.current().get("items.found", items.size)
 * }}}
 */
public class Messages {

    /**
     * Returns the Messages-instance used to access the message API for the application
     * @return a Messages instance
     */
    public static Messages current() {
        return new Messages();
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
    public String get(String key, Object... args) {
        Buffer scalaArgs = scala.collection.JavaConversions.asScalaBuffer( Arrays.asList(args));
        return play.api.i18n.Messages.apply(key, scalaArgs);
    }
}
