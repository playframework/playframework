/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;

class MessagesTest {

  // #test-messages
  @Test
  void renderMessages() {
    Langs langs = new Langs(new play.api.i18n.DefaultLangs());

    Map<String, String> messagesMap = Collections.singletonMap("foo", "bar");
    Map<String, Map<String, String>> langMap =
        Collections.singletonMap(Lang.defaultLang().code(), messagesMap);
    MessagesApi messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);

    Messages messages = messagesApi.preferred(langs.availables());
    assertEquals("bar", messages.at("foo"));
  }
  // #test-messages

}
