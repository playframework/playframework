# Small description how to write modules
Modules are the central idea to let the core of play 2.0 small on the one hand but deliver many functionality out of the box with play 2.0. You can find a good description how to do this at [objectify](http://www.objectify.be/wordpress/?p=363). This article is the base of this page, but this should become not so detailed.

## Write a module:
1. Create a directory for your module, for example permsec. 
2. Create a application with provides the functionality with `play new psec`. Because it's a module delete `routes` and  delete the content of `application.conf`, but don't delete the file.
3. Write your code, but have in mind you don't can use the routes-class. If you need configuration, put all the default-values into `conf/reference.conf`.
4. Define version and name in `project/Build.scala`
5. Call `play clean` and then `play publish-local`
Now your module is ready and locally published.

## Use your module
1. Create in the module directory here permsec a directory `samples-and-tests`.
2. Create a application which demonstrates how to use your module with `play new sample`.
3. Define the dependency to your new module in `/sample/project/Build.scala`
```
val appDependencies = Seq(
   "psec" % "psec_2.9.1" % "0.1"
)
```
4. call `reload` in the play-console. You can check the loading with the command `dependencies`

## Changes in the module
If you need further changes in your module you call in the module `play clean` and then `play publish-local`. 

## Publish your module
At the moment there is no central place to publish modules. One solution is to publish via github. 
The steps are following and based on [Objectify](http://www.objectify.be/wordpress/?p=410):

1. Create a account foo in the following `<your username>`

2. Create a module <your username>.github.com

3. Clone the module with `git clone <your username>.github.com`

4. `cd  <your username>.github.com`

5. `mkdir releases`

6. `cp -rv ${PLAY_HOME}/repository/local/hurdy <your username>.github.com/releases`

7. `git add .`

8. `git commit`

9. `git push`

Now you can easily refer your module if you add the following to your `Build.scala`
```
val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(        
      // Add your own project settings here 
      resolvers += Resolver.url("<your username> GitHub Play Repository", url("http://<your username>.github.com/releases/"))(Resolver.ivyStylePatterns)
    )
```