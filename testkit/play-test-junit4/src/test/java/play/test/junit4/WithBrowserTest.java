/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit4;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class WithBrowserTest extends WithBrowser {
  @Test
  public void withBrowserShouldProvideABrowser() {
    assertNotNull(browser);
    browser.goTo("/");
    assertThat(browser.pageSource(), containsString("Action Not Found"));
  }
}
