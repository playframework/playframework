/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.server;

import java.util.Optional;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import scala.jdk.javaapi.OptionConverters;

/** Provides information about a Play Application running inside a Play server. */
public class ApplicationProvider {

  private final Application application;
  private final play.core.ApplicationProvider underlying;

  public ApplicationProvider(Application application) {
    this.application = application;
    this.underlying = play.core.ApplicationProvider$.MODULE$.apply(application.asScala());
  }

  /**
   * @return The Scala version of this application provider.
   */
  public play.core.ApplicationProvider asScala() {
    return underlying;
  }

  /**
   * @return Returns an Optional with the application, if available.
   */
  public Optional<Application> get() {
    return Optional.ofNullable(application);
  }

  /**
   * Handle a request directly, without using the application.
   *
   * @param requestHeader the request made.
   * @deprecated Deprecated as of 2.7.0. WebCommands are now handled by the
   *     DefaultHttpRequestHandler.
   */
  @Deprecated
  public Optional<Result> handleWebCommand(Http.RequestHeader requestHeader) {
    return OptionConverters.toJava(this.underlying.handleWebCommand(requestHeader.asScala()))
        .map(play.api.mvc.Result::asJava);
  }
}
