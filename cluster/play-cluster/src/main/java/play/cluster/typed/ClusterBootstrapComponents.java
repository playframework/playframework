/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.typed;

import play.components.*;
import akka.annotation.ApiMayChange;
import play.api.cluster.typed.PlayClusterBootstrap;
import akka.actor.ActorSystem;
import play.api.Configuration;
import play.api.Environment;

@ApiMayChange
public interface ClusterBootstrapComponents extends AkkaComponents {

  ActorSystem actorSystem();

  Configuration configuration();

  Environment environment();

  default PlayClusterBootstrap playClusterBootstrap() {
    // eagerly bootstrap the cluster
    return new PlayClusterBootstrap(actorSystem(), configuration(), environment());
  }
}
