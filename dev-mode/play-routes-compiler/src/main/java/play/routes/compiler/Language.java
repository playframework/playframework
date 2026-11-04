/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler;

/** The language of the source code that the routes compiler generates. */
public enum Language {
  /** Generate Scala source code. */
  SCALA,
  /** Generate Java source code. */
  JAVA
}
