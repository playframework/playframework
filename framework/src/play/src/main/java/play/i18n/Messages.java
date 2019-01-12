/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import play.api.i18n.MessagesProvider;
import play.libs.typedmap.TypedKey;

import java.util.List;

/**
 * A Messages will produce messages using a specific language.
 *
 * This interface that is typically backed by MessagesImpl, but does not
 * return MessagesApi.
 */
public interface Messages extends MessagesProvider {

    public static class Attrs {

        public static TypedKey<play.api.i18n.Lang> CurrentLang = play.api.i18n.Messages.Attrs$.MODULE$.CurrentLang().asJava();

    }

    /**
     * Get the lang for these messages.
     *
     * @return the chosen language
     */
    public Lang lang();

    /**
     * Get the message at the given key.
     *
     * Uses `java.text.MessageFormat` internally to format the message.
     *
     * @param key the message key
     * @param args the message arguments
     * @return the formatted message or a default rendering if the key wasn't defined
     */
    default String apply(String key, Object... args) {
        return at(key, args);
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
    default String apply(List<String> keys, Object... args) {
        return at(keys, args);
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
    public String at(String key, Object... args);

    /**
     * Get the message at the first defined key.
     *
     * Uses `java.text.MessageFormat` internally to format the message.
     *
     * @param keys the messages keys
     * @param args the message arguments
     * @return the formatted message or a default rendering if the key wasn't defined
     */
    public String at(List<String> keys, Object... args);

    /**
     * Check if a message key is defined.
     *
     * @param key the message key
     * @return a Boolean
     */
    public Boolean isDefinedAt(String key);


    public play.api.i18n.Messages asScala();

    @Override
    default play.api.i18n.Messages messages() {
        return this.asScala();
    }
}
