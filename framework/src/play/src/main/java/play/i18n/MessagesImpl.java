/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import java.util.List;

/**
 * This class implements the Messages interface.
 *
 * This class serves two purposes. One is for backwards compatibility, it serves the old static API for accessing
 * messages.  The other is a new API, which carries an inject messages, and a selected language.
 *
 * The methods for looking up messages on the old API are called get, on the new API, they are called at. In Play 3.0,
 * when we remove the old API, we may alias the at methods to the get names.
 */
public class MessagesImpl implements Messages {

    private final Lang lang;
    private final MessagesApi messagesApi;

    public MessagesImpl(Lang lang, MessagesApi messagesApi) {
        this.lang = lang;
        this.messagesApi = messagesApi;
    }

    /**
     * @return the selected language for the messages.
     */
    public Lang lang() {
        return lang;
    }

    /**
     * @return The underlying API
     */
    public MessagesApi messagesApi() {
        return messagesApi;
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
        return messagesApi.get(lang, key, args);
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
        return messagesApi.get(lang, keys, args);
    }

    /**
     * Check if a message key is defined.
     *
     * @param key the message key
     * @return a Boolean
     */
    public Boolean isDefinedAt(String key) {
        return messagesApi.isDefinedAt(lang, key);
    }

    @Override
    public play.api.i18n.Messages asScala() {
        return new play.api.i18n.MessagesImpl(lang, messagesApi.asScala());
    }
}
