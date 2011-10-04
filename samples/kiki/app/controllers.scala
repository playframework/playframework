package controllers

import play.api.mvc._
import play.api.mvc.Results._

object Actions {
    
    def Secured[A](predicate:Context[A]=>Boolean)(action:Action[A]):Action[A] =  Action[A](action.con.parser, ctx => 
      {
          if(predicate(ctx)) {
                action.con.f(ctx)
          } else {
                Forbidden
          }
      })
    
    def Secured[A](predicate: =>Boolean)(action:Action[A]):Action[A] = Secured((_:Context[A]) => predicate)(action)
    
    val cache = scala.collection.mutable.HashMap.empty[String,Result] 
    
    def Cached[A](args: Any*)(action:Action[A]) = Action[A](action.con.parser, ctx =>
        {
        
            val key = args.mkString
        
            cache.get(key).getOrElse {
                val r = action.con.f(ctx)
                cache.put(key, r)
                r
            }
        } )
    
}

import Actions._

object Blocking extends Controller {

    val waited = play.core.Iteratee.Promise[Int]()

    def unblockEveryone(status:Int) = Action {
        waited.redeem(status) 
        Ok
    }

    def waitForUnblock = Action {
        AsyncResult(waited.map{ status => println("status"); EmptyStatus(status)})

    }

}

object TOTO {
    
    lazy val a = System.currentTimeMillis
    
} 

object Application extends Controller {
    
    override def Action[A](bodyParser:BodyParser[A], block:Context[A] => Result) =  super.Action(bodyParser,ctx => {
            println("Request for Application controller")
            block(ctx)
        })
    
    def coco = Action {
        NotFound("oops")
    }
    
    import play.core.Iteratee._
    
    def index = Action {
        Ok(views.html.index("World " + TOTO.a))
    }
    
    def websocketTest = Action {
        Ok(views.html.sockets())
    }
    
    def moreSockets = Action {
        Ok(views.html.moreSockets())
    }
    
    def socketEchoReversed = Action {
        SocketResult[String]{ (in,out) => 
            out <<: in.map {
                case El("") => EOF
                case o => o.map(_.reverse)
            }
        }
    }
    
    val specialTemplates = Map(
        "home"  -> views.pages.html.home.f,
        "about" -> views.pages.html.about.f
    )
    
    def page(name:String) = Action {
        Ok(specialTemplates.get(name).getOrElse(views.pages.html.page.f)(name, "Dummy content"))
    }
    
    def list(page:Int, sort:String) = {
        
        val p = if(page > 0) page else 1
        
        Secured(p != 42) { 
            Cached(p, sort) { 
                Action {
                    println("Listing page " + p + " using " + sort)
                    Ok(views.html.list(p, sort).toString)
                }
            }
        } 
    }
    
    def goToJava = Action {
        Redirect(routes.JavaApplication.index)
    }
    
    def bindOptions(p1: Option[Int], p2: Option[Int]) = Action {
      Ok( """
        params: p1=%s, p2=%s
        reversed: %s
        reversed: %s
        reversed: %s
        reversed: %s
      """.format(
        p1, p2,
        routes.Application.bindOptions(None, None),
        routes.Application.bindOptions(Some(42), None),
        routes.Application.bindOptions(None, Some(42)),
        routes.Application.bindOptions(Some(42), Some(42))
      ))
    }

}
