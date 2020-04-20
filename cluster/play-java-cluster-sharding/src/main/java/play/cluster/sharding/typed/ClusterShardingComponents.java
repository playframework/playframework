/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed;

import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import play.components.*;
import akka.annotation.ApiMayChange;

@ApiMayChange
/** Akka components for Cluster Sharding. */
public interface ClusterShardingComponents extends AkkaComponents {

  default ClusterSharding clusterSharding() {
    return new ClusterShardingProvider(actorSystem()).get();
  }
}
