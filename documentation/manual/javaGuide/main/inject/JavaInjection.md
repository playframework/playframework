By default Play binds URLs to controller methods _statically_, that is, Controller instances are created by the framework and then the appropriate static method is invoked depending on the given URL. In certain situations, however, you may want to manage controller creation and that's when the new routing syntax comes handy. 

Route definitions starting with ```@``` will be managed by ```play.GlobalSettings#getControllerInstance``` method, so given the following route definition: 

    GET     /                  @controllers.Application.index()

Play will invoke ```play.GlobalSettings#getControllerInstance``` which in return will provide an instance of ```controllers.Application``` (by default this is happening via ```controllers.Application```'s default constructor). Therefore, if you want to manage controller class instantiation either via a dependency injection framework or manually you can do so by overriding ```getControllerInstance``` in your application's ```Global``` class.

Here's an example using Guice:

```java
    import play.GlobalSettings;

    import com.google.inject.Guice;
    import com.google.inject.Injector;

    public class Global extends GlobalSettings {

      private static final Injector INJECTOR = createInjector(); 

      @Override
      public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return INJECTOR.getInstance(controllerClass);
      }

      private static Injector createInjector() {
        return Guice.createInjector();
      }

    }
```

another example using Spring:
https://github.com/guillaumebort/play20-spring-demo
