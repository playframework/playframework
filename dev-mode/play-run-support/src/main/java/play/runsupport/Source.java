/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.io.File;

public class Source {
  File file;

  File original;

  public Source(File file, File original) {
    this.file = file;
    this.original = original;
  }
}
