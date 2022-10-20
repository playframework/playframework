/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import com.typesafe.config.ConfigFactory;
import java.util.function.Supplier;
import play.api.Configuration;
import play.api.http.DefaultFileMimeTypes;
import play.api.http.FileMimeTypesConfiguration;
import play.api.http.HttpConfiguration;
import play.libs.F;

public class StaticFileMimeTypes {
  private static FileMimeTypes mimeTypes = null;
  private static Supplier<FileMimeTypes> defaultFileMimeTypes =
      F.LazySupplier.lazy(StaticFileMimeTypes::newDefaultFileMimeTypes);

  public static FileMimeTypes newDefaultFileMimeTypes() {
    Configuration config = new Configuration(ConfigFactory.load());
    FileMimeTypesConfiguration fileMimeTypesConfiguration =
        new FileMimeTypesConfiguration(HttpConfiguration.parseFileMimeTypes(config));
    return new FileMimeTypes(new DefaultFileMimeTypes(fileMimeTypesConfiguration));
  }

  public static void setFileMimeTypes(FileMimeTypes fileMimeTypes) {
    mimeTypes = fileMimeTypes;
  }

  public static FileMimeTypes fileMimeTypes() {
    if (mimeTypes == null) {
      return defaultFileMimeTypes.get();
    } else {
      return mimeTypes;
    }
  }
}
