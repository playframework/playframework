In Play, routes which start with ```@``` will be managed by ```play.GlobalSettings#getControllerInstance``` method, so given the following route definition: 

    GET     /                  @controllers.Application.index()

Play will invoke ```play.GlobalSettings#getControllerInstance``` which in return will provide an instance of ```controllers.Application``` (by default this is happening via ```controllers.Application```'s default constructor). Therefore, if you want to manage controller class instantiation either via a dependency injection framework or manually you can do so by overriding ```getControllerInstance``` in your application's ```Global``` class.

Here's an example using Guice:
```
    import play.api.GlobalSettings

    import com.google.inject.Guice
    import com.google.inject.Injector

    object Global extends GlobalSettings {

      val INJECTOR: Injector = createInjector()

      override def getControllerInstance(controllerClass: Class[A]): A = {
        INJECTOR.getInstance(controllerClass)
      }  

      def createInjector(): Injector = {
          Guice.createInjector()
      }
    
    }

```
another example using Spring:
https://github.com/guillaumebort/play20-spring-demo
