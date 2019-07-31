package javaguide.akka;

// #cluster-sharding
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.controllers.AssetsComponents;
import play.routing.Router;
import play.javadsl.cluster.sharding.typed.ClusterShardingComponents;

public class ComponentsWithClusterSharding extends BuiltInComponentsFromContext
    implements ClusterShardingComponents, AssetsComponents {

  public ComponentsWithClusterSharding(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
// #cluster-sharding
