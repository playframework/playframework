## Using a separated execution context

You may want to use a separated execution context to avoid blocking threads in the default thread pool.

The only things you have to do is to use the SlickController trait with a SlickAction : 

```scala
object Application extends Controller with SlickController { 
  def list(page: Int, orderBy: Int, filter: String) = SlickAction {
    ...
  }
}
```

Then you can configure the slick thread pool in your application.conf file : 

```
slick {
  execution-context {
    fork-join-executor {
      parallelism-factor = 20.0
      parallelism-max = 200
    }
  }
}
```

You can of course tune the number of threads, depending on your needs.