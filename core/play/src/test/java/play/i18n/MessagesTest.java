/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

public class MessagesTest {

  @Test
  public void testMessageCall() {
    MessagesApi messagesApi = mock(MessagesApi.class);
    Lang lang = Lang.forCode("en-US");
    MessagesImpl messages = new MessagesImpl(lang, messagesApi);

    when(messagesApi.get(lang, "hello.world")).thenReturn("hello world!");

    String actual = messages.at("hello.world");
    String expected = "hello world!";
    assertThat(actual).isEqualTo(expected);

    verify(messagesApi).get(lang, "hello.world");
  }
}
