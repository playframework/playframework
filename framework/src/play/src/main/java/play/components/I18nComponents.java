/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.components;

import play.i18n.Langs;
import play.i18n.MessagesApi;

/**
 * Java I18n components.
 *
 * @see MessagesApi
 * @see Langs
 */
public interface I18nComponents {

    /**
     * @return an instance of MessagesApi.
     */
    MessagesApi messagesApi();

    /**
     * @return an instance of Langs.
     */
    Langs langs();
}
