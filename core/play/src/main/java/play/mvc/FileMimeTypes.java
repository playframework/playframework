/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import scala.jdk.javaapi.OptionConverters;

@Singleton
public class FileMimeTypes {

  private final play.api.http.FileMimeTypes fileMimeTypes;

  @Inject
  public FileMimeTypes(play.api.http.FileMimeTypes fileMimeTypes) {
    this.fileMimeTypes = fileMimeTypes;
  }

  public Optional<String> forFileName(String name) {
    return OptionConverters.toJava(fileMimeTypes.forFileName(name));
  }

  public play.api.http.FileMimeTypes asScala() {
    return fileMimeTypes;
  }
}
