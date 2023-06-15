/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.akkahttp

import org.apache.pekko.http.scaladsl.model.headers.Host
import play.api.Configuration

private[server] final class PekkoServerConfigReader(akkaServerConfig: Configuration) {
  def getHostHeader: Either[Throwable, Host] = {
    Host
      .parseFromValueString(akkaServerConfig.get[String]("default-host-header"))
      .left
      .map { errors =>
        akkaServerConfig.reportError(
          "default-host-header",
          "Couldn't parse default host header",
          Some(new RuntimeException(errors.map(_.formatPretty).mkString(", ")))
        )
      }
  }
}
