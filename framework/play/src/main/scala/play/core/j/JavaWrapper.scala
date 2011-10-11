package play.core.j

import scala.collection.JavaConverters._

import play.api.mvc._

import play.mvc.{Action => JAction, Result => JResult}
import play.mvc.Http.{Context => JContext, Request => JRequest}

trait JavaAction extends Action[AnyContent] {
    
    def parser = play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */)
    
    def invocation:JResult
    def controller:Class[_]
    def method:java.lang.reflect.Method
    
    def apply(req:Request[AnyContent]) = {
        
        val javaContext = new JContext(
            
            new JRequest {
                
                def uri = req.uri
                def method = req.method
                def path = req.method
                
                def urlFormEncoded = {
                    req.body.urlFormEncoded.mapValues(_.toArray).asJava
                }
                
                override def toString = req.toString
                
            },
            
            req.session.data.asJava
            
        )
        
        val rootAction = new JAction[Any] {
            
            def call(ctx:JContext):JResult = {
                try {
                    JContext.current.set(ctx)
                    invocation
                } finally {
                    JContext.current.remove()
                }
            }
        } 
        
        val actionMixins = {
            (method.getDeclaredAnnotations ++ controller.getDeclaredAnnotations)
                .filter(_.annotationType.isAnnotationPresent(classOf[play.mvc.With]))
                .map(a => a -> a.annotationType.getAnnotation(classOf[play.mvc.With]).value())
                .reverse
        }

        val finalAction = actionMixins.foldLeft(rootAction) { 
            case (deleguate, (annotation, actionClass)) => {
                val action = actionClass.newInstance().asInstanceOf[JAction[Any]]
                action.configuration = annotation
                action.deleguate = deleguate
                action
            }
        }
        
        finalAction.call(javaContext).getWrappedResult match {
            case result@SimpleResult(_,_) => {
                result
                    .withHeaders(javaContext.response.getHeaders.asScala.toSeq:_*)
                    .withSession(Session(javaContext.session.asScala.toMap))
            }
            case other => other
        }
    }
    
}
