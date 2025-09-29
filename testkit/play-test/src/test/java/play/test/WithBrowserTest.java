/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.Test;

public class WithBrowserTest extends WithBrowser {
  @Test
  public void withBrowserShouldProvideABrowser() {
    assertNotNull(browser);
    open("/"); // browser.goTo("/");
    assertThat(WebDriverRunner.source()).contains("Action Not Found");
  }
}
