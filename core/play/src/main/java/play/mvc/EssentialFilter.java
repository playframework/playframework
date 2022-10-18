/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

public abstract class EssentialFilter implements play.api.mvc.EssentialFilter {
  public abstract EssentialAction apply(play.mvc.EssentialAction next);

  @Override
  public play.mvc.EssentialAction apply(play.api.mvc.EssentialAction next) {
    return apply(next.asJava());
  }

  @Override
  public EssentialFilter asJava() {
    return this;
  }

  public play.api.mvc.EssentialFilter asScala() {
    return this;
  }
}
