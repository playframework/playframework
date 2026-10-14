/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.io.Closeable;
import play.core.BuildLink;

/** Dev server */
public interface DevServer extends Closeable {
  BuildLink buildLink();

  /** Compiles the application without loading it. The next request uses the compilation result. */
  void compile();

  /** Reloads the application. */
  void reload();
}
