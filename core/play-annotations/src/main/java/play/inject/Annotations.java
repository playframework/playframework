/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import javax.inject.Named;

public final class Annotations {

  private Annotations() {}

  public static Named named(String value) {
    return new play.inject.NamedImpl(value);
  }
}
