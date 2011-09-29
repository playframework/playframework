package play.core.server
import play.api._
import play.core._
import play.api.mvc._

trait Server {
    
    import akka.actor._
    import akka.actor.Actor._
    import akka.routing.Routing._
    import akka.routing.SmallestMailboxFirstIterator
    import akka.config._
    import akka.config.Supervision._

    def newInvoker = {val inv = actorOf[Invoker]; inv.start(); inv}
    
    val invoker = loadBalancerActor(new SmallestMailboxFirstIterator(List.fill(3)(newInvoker))).start()

    def getActionFor(rqHeader:RequestHeader):Either[Result,(Action,Application)] = {
         def sendAction :Either[Throwable,(Action,Application)]=
            applicationProvider.get.right.map { application => 
                            val maybeAction = application.global.onRouteRequest(rqHeader)
                           ( maybeAction.getOrElse(Action(_ => application.global.onActionNotFound(rqHeader))),application ) }
                       

      import scala.util.control.Exception
       applicationProvider.handleWebCommand(rqHeader).toLeft{
          Exception.allCatch[Either[Throwable,(Action,Application)]]
                   .either (sendAction)
                   .joinRight
                   .left.map(e =>  DefaultGlobal.onError(e)) }.joinRight

      
    }
    def errorResult(e:Throwable):Result = DefaultGlobal.onError(e)

    def invoke[A](request:Request1[A], response:Response,a:(Context[A]=>Result), app:Application) = invoker ! HandleAction(request,response,a,app)

    
    def applicationProvider:ApplicationProvider
    
}
