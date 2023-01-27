/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed;

import akka.annotation.ApiMayChange;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import play.components.*;

@ApiMayChange
/** Akka components for Cluster Sharding. */
public interface ClusterShardingComponents extends AkkaComponents {

  default ClusterSharding clusterSharding() {
    return new ClusterShardingProvider(actorSystem()).get();
  }
}
