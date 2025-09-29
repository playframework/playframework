/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import static com.codeborne.selenide.Selenide.*;
import static org.junit.Assert.*;

import org.junit.*;
import play.test.*;

// #test-withbrowser
public class BrowserFunctionalTest extends WithBrowser {

  @Test
  public void runInBrowser() {
    open("/");
    assertNotNull($("title").text());
  }
}
// #test-withbrowser
