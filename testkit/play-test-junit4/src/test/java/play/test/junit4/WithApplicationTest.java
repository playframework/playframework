/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit4;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import play.i18n.MessagesApi;

/** Tests WithApplication functionality. */
public class WithApplicationTest extends WithApplication {

  @Test
  public void shouldHaveAnAppInstantiated() {
    assertNotNull(app);
  }

  @Test
  public void shouldHaveAMaterializerInstantiated() {
    assertNotNull(mat);
  }

  @Test
  public void withInstanceOf() {
    MessagesApi messagesApi = instanceOf(MessagesApi.class);
    assertNotNull(messagesApi);
  }
}
