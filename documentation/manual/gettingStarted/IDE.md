<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Setting up your preferred IDE

Working with Play is easy. You don’t even need a sophisticated IDE, because Play compiles and refreshes the modifications you make to your source files automatically, so you can easily work using a simple text editor.

However, using a modern Java or Scala IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging.

## Eclipse

### Generate configuration

Play provides a command to simplify Eclipse configuration. To transform a Play application into a working Eclipse project, use the `eclipse` command:

```
[my-first-app] $ eclipse
```

If you want to grab the available source jars (this will take longer and it's possible a few sources might be missing):

```
[my-first-app] $ eclipse with-source=true
```

> Note if you are using sub-projects with aggregate, you would need to set `skipParents` appropriately in `build.sbt`:

```
EclipseKeys.skipParents in ThisBuild := false
```

or from the play console, type:

``` 
[my-first-app] $ eclipse skip-parents=false
```

> Also, if you did not want to trigger a compilation before running `eclipse`, then just add the following to your settings:

```
EclipseKeys.preTasks := Seq()
```

You then need to import the application into your Workspace with the **File/Import/General/Existing project…** menu (compile your project first).

[[images/eclipse.png]] 

To debug, start your application with `activator -jvm-debug 9999 run` and in Eclipse right-click on the project and select **Debug As**, **Debug Configurations**. In the **Debug Configurations** dialog, right-click on **Remote Java Application** and select **New**. Change **Port** to 9999 and click **Apply**. From now on you can click on **Debug** to connect to the running application. Stopping the debugging session will not stop the server.

> **Tip**: You can run your application using `~run` to enable direct compilation on file change. This way scala template files are auto discovered when you create a new template in `view` and auto compiled when the file changes. If you use normal `run` then you have to hit `Refresh` on your browser each time.

If you make any important changes to your application, such as changing the classpath, use `eclipse` again to regenerate the configuration files.

> **Tip**: Do not commit Eclipse configuration files when you work in a team!

The generated configuration files contain absolute references to your framework installation. These are specific to your own installation. When you work in a team, each developer must keep his Eclipse configuration files private.

## IntelliJ

Intellij IDEA lets you quickly create a Play application without using a command prompt. You don't need to configure anything outside of the IDE, the SBT build tool takes care of downloading appropriate libraries, resolving dependencies and building the project.

Before you start creating a Play application in IntelliJ IDEA, make sure that the latest Scala (if you develop with Scala) and 
Play 2 plugins are enabled in IntelliJ IDEA.

To create a Play application:

- Open ***New Project*** wizard, select ***Play 2.x*** under ***Scala*** section and click ***Next***.
- Enter your project's information and click ***Finish***.

IntelliJ IDEA creates an empty application using SBT.

You can also import an existing Play project.

To import a Play project:
- Open Project wizard, select ***Import Project***.
- In the window that opens, select a project you want to import and click ***OK***.
- On the next page of the wizard, select ***Import project from external model*** option, choose ***SBT project*** and click ***Next***. 
- On the next page of the wizard, select additional import options and click ***Finish***. 

Check the project's structure, make sure all necessary dependencies are downloaded.
You can use code assistance, navigation and on-the-fly code analysis features.

You can run the created application and view the result in the default browser `http://localhost:9000`. 

To run a Play application:
-	In the project tree, right-click the application.
-	From the list in the context menu, select ***Run Play2 App***.

You can easily start a debugger session for a Play application using default Run/Debug Configuration settings.

For more detailed information, see the Play Framework 2.x tutorial at the following URL:

<http://confluence.jetbrains.com/display/IntelliJIDEA/Play+Framework+2.0> 

### Navigate from an error page to the source code
Using the `play.editor` configuration option, you can set up Play to add hyperlinks to an error page. Since then, you can easily navigate from error pages to IntelliJ, directly into the source code (you need to install the Remote Call <https://github.com/Zolotov/RemoteCall> IntelliJ plugin first).

Just install the Remote Call plugin and run your app with the following options:
`-Dplay.editor=http://localhost:8091/?message=%s:%s -Dapplication.mode=dev`


## Netbeans

### Generate Configuration

Play does not have native Netbeans project generation support at this time.


## ENSIME

### Install ENSIME

Follow the installation instructions at <https://github.com/ensime/ensime-emacs>

### Generate configuration

Edit your project/plugins.sbt file, and add the following line (you should first check <https://github.com/ensime/ensime-sbt> for the latest version of the plugin):

```
addSbtPlugin("org.ensime" % "ensime-sbt" % "0.1.5-SNAPSHOT")
```

Start Play:

```
$ activator
```

Enter 'ensime generate' at the play console. The plugin should generate a .ensime file in the root of your Play project.

```
$ [MYPROJECT] ensime generate
[info] Gathering project information...
[info] Processing project: ProjectRef(file:/Users/aemon/projects/www/MYPROJECT/,MYPROJECT)...
[info]  Reading setting: name...
[info]  Reading setting: organization...
[info]  Reading setting: version...
[info]  Reading setting: scala-version...
[info]  Reading setting: module-name...
[info]  Evaluating task: project-dependencies...
[info]  Evaluating task: unmanaged-classpath...
[info]  Evaluating task: managed-classpath...
[info] Updating {file:/Users/aemon/projects/www/MYPROJECT/}MYPROJECT...
[info] Done updating.
[info]  Evaluating task: internal-dependency-classpath...
[info]  Evaluating task: unmanaged-classpath...
[info]  Evaluating task: managed-classpath...
[info]  Evaluating task: internal-dependency-classpath...
[info] Compiling 5 Scala sources and 1 Java source to /Users/aemon/projects/www/MYPROJECT/target/scala-2.9.1/classes...
[info]  Evaluating task: exported-products...
[info]  Evaluating task: unmanaged-classpath...
[info]  Evaluating task: managed-classpath...
[info]  Evaluating task: internal-dependency-classpath...
[info]  Evaluating task: exported-products...
[info]  Reading setting: source-directories...
[info]  Reading setting: source-directories...
[info]  Reading setting: class-directory...
[info]  Reading setting: class-directory...
[info]  Reading setting: ensime-config...
[info] Wrote configuration to .ensime
```

### Start ENSIME

From Emacs, execute M-x ensime and follow the on-screen instructions.

That's all there is to it. You should now get type-checking, completion, etc. for your Play project. Note, if you add new library dependencies to your play project, you'll need to re-run "ensime generate" and re-launch ENSIME.

### More Information

Check out the ENSIME README at <https://github.com/ensime/ensime-emacs>.
If you have questions, post them in the ensime group at <https://groups.google.com/forum/?fromgroups=#!forum/ensime>.


## All Scala Plugins if needed

Scala is a newer programming language, so the functionality is provided in plugins rather than in the core IDE.

- Eclipse Scala IDE: <http://scala-ide.org/>
- NetBeans Scala Plugin: <https://java.net/projects/nbscala>
- IntelliJ IDEA Scala Plugin: <http://confluence.jetbrains.net/display/SCA/Scala+Plugin+for+IntelliJ+IDEA>
- IntelliJ IDEA's plugin is under active development, and so using the nightly build may give you additional functionality at the cost of some minor hiccups.
- Nika (11.x) Plugin Repository: <http://www.jetbrains.com/idea/plugins/scala-nightly-nika.xml>
- Leda (12.x) Plugin Repository: <http://www.jetbrains.com/idea/plugins/scala-nightly-leda.xml>
- IntelliJ IDEA Play plugin (available only for Leda 12.x): <http://plugins.intellij.net/plugin/?idea&pluginId=7080>
- ENSIME - Scala IDE Mode for Emacs: <https://github.com/aemoncannon/ensime>
(see below for ENSIME/Play instructions)
