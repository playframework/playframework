/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed;

import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import play.components.*;
import akka.annotation.ApiMayChange;

@ApiMayChange
public interface ClusterShardingComponents extends AkkaComponents {

  default ClusterSharding clusterSharding() {
    return new ClusterShardingProvider(actorSystem()).get();
  }
}
