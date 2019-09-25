
package scalaguide.akka.components

//#cluster-compile-time-injection
import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.scaladsl.cluster.sharding.typed.ClusterShardingComponents

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class ComponentsWithClusterSharding(context: Context) 
  extends BuiltInComponentsFromContext(context) 
    with ClusterShardingComponents {
      
  lazy val router = Router.empty
}
//#cluster-compile-time-injection