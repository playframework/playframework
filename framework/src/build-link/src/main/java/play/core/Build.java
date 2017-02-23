/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class Build {

  public static final List<String> sharedClasses;
  static {
    List<String> list = new ArrayList<String>();
    list.add(play.core.BuildLink.class.getName());
    list.add(play.core.BuildDocHandler.class.getName());
    list.add(play.core.server.ServerWithStop.class.getName());
    list.add(play.api.UsefulException.class.getName());
    list.add(play.api.PlayException.class.getName());
    list.add(play.api.PlayException.InterestingLines.class.getName());
    list.add(play.api.PlayException.RichDescription.class.getName());
    list.add(play.api.PlayException.ExceptionSource.class.getName());
    list.add(play.api.PlayException.ExceptionAttachment.class.getName());
    sharedClasses = Collections.unmodifiableList(list);
  }

}
