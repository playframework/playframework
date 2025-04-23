/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

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
