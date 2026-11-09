/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TemplateReverseRoutesTest {
  @Test
  public void callReverseRoutesFromTemplates() {
    String body = views.html.reverseRoutes.render().body();
    assertThat(body.lines().map(String::trim).filter(line -> !line.isEmpty()))
        .containsExactly("/result/async?x=10", "/bool-d", "/public/css/abcd1234-main.css");
  }
}
