/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

// #test-assertj
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AssertJTest {

  @Test
  public void testString() {
    String str = "good";
    assertThat(str).isEqualTo("good").startsWith("goo").contains("oo");
  }
}
// #test-assertj
