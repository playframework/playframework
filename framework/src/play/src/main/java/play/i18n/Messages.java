/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.i18n;

import org.apache.commons.lang3.ArrayUtils;
import scala.collection.mutable.Buffer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import play.api.i18n.Lang;

/**
 * High-level internationalisation API.
 */
public class Messages {

    private static Lang getLang(){
        Lang lang = null;
        if(play.mvc.Http.Context.current.get() != null) {
            lang = play.mvc.Http.Context.current().lang();
        } else {
            Locale defaultLocale = Locale.getDefault();
            lang = new Lang(defaultLocale.getLanguage(), defaultLocale.getCountry());
        }
        return lang;
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
    */
    public static String get(Lang lang, String key, Object... args) {
        Buffer<Object> scalaArgs = convertArgsToScalaBuffer(args);
        return play.api.i18n.Messages.apply(key, scalaArgs, lang);
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
    */
    public static String get(Lang lang, List<String> keys, Object... args) {
        Buffer<String> keyArgs = scala.collection.JavaConverters.asScalaBufferConverter(keys).asScala();
        Buffer<Object> scalaArgs = convertArgsToScalaBuffer(args);
        return play.api.i18n.Messages.apply(keyArgs.toSeq(), scalaArgs, lang);
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
        Buffer<Object> scalaArgs = convertArgsToScalaBuffer(args);
        return play.api.i18n.Messages.apply(key, scalaArgs, getLang());
    }

    /**
    * Translates the first defined message.
    *
    * Uses `java.text.MessageFormat` internally to format the message.
    *
    * @param keys the messages keys
    * @param args the message arguments
    * @return the formatted message or a default rendering if the key wasn't defined
    */
    public static String get(List<String> keys, Object... args) {
        Buffer<String> keyArgs = scala.collection.JavaConverters.asScalaBufferConverter(keys).asScala();
        Buffer<Object> scalaArgs = convertArgsToScalaBuffer(args);
        return play.api.i18n.Messages.apply(keyArgs.toSeq(), scalaArgs, getLang());
    }

    /**
    * Check if a message key is defined.
    * @param lang the message lang
    * @param key the message key
    * @return a Boolean
    */
    public static Boolean isDefined(Lang lang, String key) {
        return play.api.i18n.Messages.isDefinedAt(key, lang);
    }

    /**
    * Check if a message key is defined.
    * @param key the message key
    * @return a Boolean
    */
    public static Boolean isDefined(String key) {
        return play.api.i18n.Messages.isDefinedAt(key, getLang());
    }
}
