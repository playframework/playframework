/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

public class ServerStartException extends IllegalStateException {

  public ServerStartException(Throwable underlying) {
    super(underlying);
  }

  @Override
  public String getMessage() {
    return getCause().getMessage();
  }
}
