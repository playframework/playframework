/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.i18n;

import org.apache.commons.lang3.ArrayUtils;
import play.api.Application;
import scala.collection.mutable.Buffer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A messages and a language.
 *
 * This class serves two purposes. One is for backwards compatibility, it serves the old static API for accessing
 * messages.  The other is a new API, which carries an inject messages, and a selected language.
 *
 * The methods for looking up messages on the old API are called get, on the new API, they are called at. In Play 3.0,
 * when we remove the old API, we may alias the at methods to the get names.
 */
public class Messages {

    // All these methods below will be removed once we get rid of the global state
    private static Lang getLang(){
        Lang lang = null;
        if(play.mvc.Http.Context.current.get() != null) {
            lang = play.mvc.Http.Context.current().lang();
        } else {
            lang = new Lang(new play.api.i18n.Lang(Locale.getDefault()));
        }
        return lang;
    }

    private static Messages getMessages(Lang lang) {
        Application app = play.api.Play.current();
        MessagesApi scalaApi = app.injector().instanceOf(MessagesApi.class);
        return new Messages(lang, scalaApi);
    }

    /**
     * Converts the varargs to a scala buffer,
     * takes care of wrapping varargs into a intermediate list if necessary
     *
     * @param args the message arguments
     * @return scala type for message processing
     */
    private static Buffer<Object> convertArgsToScalaBuffer(final Object... args) {
        return scala.collection.JavaConverters.asScalaBufferConverter(wrapArgsToListIfNeeded(args)).asScala();
    }

    /**
     * Wraps arguments passed into a list if necessary.
     *
     * Returns the first value as is if it is the only argument and a subtype of `java.util.List`
     * Otherwise, it calls Arrays.asList on args
     * @param args arguments as a List
     */
    static <T> List<T> wrapArgsToListIfNeeded(final T... args) {
        List<T> out = null;
        if (ArrayUtils.isNotEmpty(args)
            && args.length == 1
            && args[0] instanceof List){
            out = (List<T>) args[0];
        }else{
            out = Arrays.asList(args);
        }
        return out;
    }

    /**
    * Translates a message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param lang the message lang
    * @param key the message key
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
     * @deprecated Please use messages.at(key, args). since 2.5.0
    */
    @Deprecated
    public static String get(Lang lang, String key, Object... args) {
        return getMessages(lang).at(key, args);
    }

    /**
    * Translates the first defined message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param lang the message lang
    * @param keys the messages keys
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
     * @deprecated Please use messages.at(keys, args)
    */
    @Deprecated
    public static String get(Lang lang, List<String> keys, Object... args) {
        return getMessages(lang).at(keys, args);
    }

    /**
    * Translates a message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param key the message key
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
     * @deprecated use messages.at(key, args).  Deprecated since 2.5.0
    */
    @Deprecated
    public static String get(String key, Object... args) {
        return getMessages(getLang()).at(key, args);
    }

    /**
    * Translates the first defined message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param keys the messages keys
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
     * @deprecated use messages.at(keys, args).  Deprecated since 2.5.0
    */
    @Deprecated
    public static String get(List<String> keys, Object... args) {
        return getMessages(getLang()).at(keys, args);
    }

    /**
    * Check if a message key is defined.
    * @param lang the message lang
    * @param key the message key
    * @return a Boolean
     * @deprecated Use messages.isDefinedAt(key).  Deprecated since 2.5.0
    */
    @Deprecated
    public static Boolean isDefined(Lang lang, String key) {
        return getMessages(lang).isDefinedAt(key);
    }

    /**
    * Check if a message key is defined.
    * @param key the message key
    * @return a Boolean
     * @deprecated Use messages.isDefinedAt(key).  Deprecated since 2.5.0
    */
    @Deprecated
    public static Boolean isDefined(String key) {
        return getMessages(getLang()).isDefinedAt(key);
    }

    // All these methods are the new API
    private final Lang lang;
    private final MessagesApi messages;

    public Messages(Lang lang, MessagesApi messages) {
        this.lang = lang;
        this.messages = messages;
    }

    /**
     * The lang for these messages
     *
     * @return the language
     */
    public Lang lang() {
        return lang;
    }

    /**
     * @return The underlying API
     */
    public MessagesApi messagesApi() {
        return messages;
    }

    /**
     * Get the message at the given key.
     *
     * Uses `java.text.MessageFormat` internally to format the message.
     *
     * @param key the message key
     * @param args the message arguments
     * @return the formatted message or a default rendering if the key wasn't defined
     */
    public String at(String key, Object... args) {
        return messages.get(lang, key, args);
    }

    /**
     * Get the message at the first defined key.
     *
     * Uses `java.text.MessageFormat` internally to format the message.
     *
     * @param keys the messages keys
     * @param args the message arguments
     * @return the formatted message or a default rendering if the key wasn't defined
     */
    public String at(List<String> keys, Object... args) {
        return messages.get(lang, keys, args);
    }

    /**
     * Check if a message key is defined.
     *
     * @param key the message key
     * @return a Boolean
     */
    public Boolean isDefinedAt(String key) {
        return messages.isDefinedAt(lang, key);
    }

}
