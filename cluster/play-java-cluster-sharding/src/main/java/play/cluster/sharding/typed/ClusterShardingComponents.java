/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed;

import org.apache.pekko.annotation.ApiMayChange;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import play.components.*;

@ApiMayChange
/** Pekko components for Cluster Sharding. */
public interface ClusterShardingComponents extends PekkoComponents {

  default ClusterSharding clusterSharding() {
    return new ClusterShardingProvider(actorSystem()).get();
  }
}
