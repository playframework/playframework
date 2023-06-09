/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit5;

import akka.stream.Materializer;
import java.util.Objects;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import play.Application;
import play.test.Helpers;

public class ApplicationExtension implements BeforeAllCallback, AfterAllCallback {
  private final Application application;

  public ApplicationExtension(Application application) {
    this.application = application;
  }

  public Application getApplication() {
    return application;
  }

  public Materializer getMaterializer() {
    return application.asScala().materializer();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (Objects.nonNull(application)) {
      Helpers.stop(application);
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (Objects.nonNull(application)) {
      Helpers.start(application);
    }
  }
}
