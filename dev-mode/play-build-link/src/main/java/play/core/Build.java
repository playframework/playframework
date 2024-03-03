/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import java.util.List;

public class Build {

  public static final List<String> sharedClasses =
      List.of(
          play.core.BuildLink.class.getName(),
          play.core.BuildDocHandler.class.getName(),
          play.core.server.ReloadableServer.class.getName(),
          play.api.UsefulException.class.getName(),
          play.api.PlayException.class.getName(),
          play.api.PlayException.InterestingLines.class.getName(),
          play.api.PlayException.RichDescription.class.getName(),
          play.api.PlayException.ExceptionSource.class.getName(),
          play.api.PlayException.ExceptionAttachment.class.getName());
}
