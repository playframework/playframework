/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler;

/** The implementation language used for generated router source files. */
public enum Language {
  /** Generate Java router sources. */
  JAVA,

  /** Generate Scala router sources. */
  SCALA
}
