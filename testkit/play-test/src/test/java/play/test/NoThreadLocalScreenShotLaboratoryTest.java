/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class NoThreadLocalScreenShotLaboratoryTest {

  private static String counter(String fileName) {
    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }

  @Test
  public void browsersDoNotGenerateTheSameFileNames() {
    NoThreadLocalScreenShotLaboratory first = new NoThreadLocalScreenShotLaboratory();
    NoThreadLocalScreenShotLaboratory second = new NoThreadLocalScreenShotLaboratory();

    // file names are "<timestamp>.<counter>", so the counters must differ for the same timestamp
    assertThat(counter(second.generateScreenshotFileName()))
        .isNotEqualTo(counter(first.generateScreenshotFileName()));
  }
}
