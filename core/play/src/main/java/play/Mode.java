/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

/** Application mode, either `DEV`, `TEST`, or `PROD`. */
public enum Mode {
  DEV,
  TEST,
  PROD;

  public play.api.Mode asScala() {
    if (this == DEV) {
      return play.api.Mode.Dev$.MODULE$;
    } else if (this == PROD) {
      return play.api.Mode.Prod$.MODULE$;
    }
    return play.api.Mode.Test$.MODULE$;
  }
}
