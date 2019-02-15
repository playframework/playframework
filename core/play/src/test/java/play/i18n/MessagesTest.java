/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import org.junit.Test;

import static org.mockito.Mockito.*;

import static org.fest.assertions.Assertions.assertThat;

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
