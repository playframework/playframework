<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Setting up your preferred IDE

Working with Play is easy. You don’t even need a sophisticated IDE, because Play compiles and refreshes the modifications you make to your source files automatically, so you can easily work using a simple text editor.

However, using a modern Java or Scala IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging.

## Eclipse

### Setup sbteclipse

Integration with Eclipse requires [sbteclipse](https://github.com/typesafehub/sbteclipse). Make sure to always use the [most recent available version](https://github.com/typesafehub/sbteclipse/releases).Add the below code to the plugin.sbt file.

```scala
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.2")
```

You must `compile` your project before running the `eclipse` command. You can force compilation to happen when the `eclipse` command is run by adding the following setting:

```scala
// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
```

If you have Scala sources in your project, you will need to install [Scala IDE](http://scala-ide.org/).

If you do not want to install Scala IDE and have only Java sources in your project, then you can set the following:

```scala
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java           // Java project. Don't expect Scala IDE
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)  // Use .class files instead of generated .scala files for views and routes
```

### Generate configuration

Play provides a command to simplify [Eclipse](https://eclipse.org/) configuration. To transform a Play application into a working Eclipse project, use the `eclipse` command:

```bash
[my-first-app] $ eclipse
```

If you want to grab the available source jars (this will take longer and it's possible a few sources might be missing):

```bash
[my-first-app] $ eclipse with-source=true
```

> Note if you are using sub-projects with aggregate, you would need to set `skipParents` appropriately in `build.sbt`:

```scala
EclipseKeys.skipParents in ThisBuild := false
```

or from the [sbt shell](http://www.scala-sbt.org/0.13/docs/Howto-Interactive-Mode.html), type:

```bash
[my-first-app] $ eclipse skip-parents=false
```

You then need to import the application into your Workspace with the **File/Import/General/Existing project…** menu (compile your project first).

[[images/eclipse.png]]

To debug, start your application with `sbt -jvm-debug 9999 run` and in Eclipse right-click on the project and select **Debug As**, **Debug Configurations**. In the **Debug Configurations** dialog, right-click on **Remote Java Application** and select **New**. Change **Port** to 9999 and click **Apply**. From now on you can click on **Debug** to connect to the running application. Stopping the debugging session will not stop the server.

> **Tip**: You can run your application using `~run` to enable direct compilation on file change. This way scala template files are auto discovered when you create a new template in `view` and auto compiled when the file changes. If you use normal `run` then you have to hit `Refresh` on your browser each time.

If you make any important changes to your application, such as changing the classpath, use `eclipse` again to regenerate the configuration files.

> **Tip**: Do not commit Eclipse configuration files when you work in a team!

The generated configuration files contain absolute references to your framework installation. These are specific to your own installation. When you work in a team, each developer must keep his Eclipse configuration files private.

## IntelliJ IDEA

[Intellij IDEA](https://www.jetbrains.com/idea/) lets you quickly create a Play application without using a command prompt. You don't need to configure anything outside of the IDE, the SBT build tool takes care of downloading appropriate libraries, resolving dependencies and building the project.

Before you start creating a Play application in IntelliJ IDEA, make sure that the latest [Scala Plugin](https://www.jetbrains.com/idea/help/creating-and-running-your-scala-application.html) is installed and enabled in IntelliJ IDEA. Even if you don't develop in Scala, it will help with the template engine and also resolving dependencies.

To create a Play application:

1. Open ***New Project*** wizard, select ***Sbt*** under ***Scala*** section and click ***Next***.
2. Enter your project's information and click ***Finish***.

You can also import an existing Play project.

To import a Play project:

1. Open Project wizard, select ***Import Project***.
2. In the window that opens, select a project you want to import and click ***OK***.
3. On the next page of the wizard, select ***Import project from external model*** option, choose ***SBT project*** and click ***Next***.
4. On the next page of the wizard, select additional import options and click ***Finish***.

> **Tip**: you can download and import one of our [starter projects](https://playframework.com/download#starters) or either one of the [example projects](https://playframework.com/download#examples).

Check the project's structure, make sure all necessary dependencies are downloaded. You can use code assistance, navigation and on-the-fly code analysis features.

You can run the created application and view the result in the default browser `http://localhost:9000`. To run a Play application:

1. Create a new Run Configuration -- From the main menu, select Run -> Edit Configurations
2. Click on the + to add a new configuration
3. From the list of configurations, choose "SBT Task"
4. In the "tasks" input box, simply put "run"
5. Apply changes and select OK.
6. Now you can choose "Run" from the main Run menu and run your application

You can easily start a debugger session for a Play application using default Run/Debug Configuration settings.

For more detailed information, see the Play Framework 2.x tutorial at the following URL:

<https://www.jetbrains.com/idea/help/getting-started-with-play-2-x.html>

### Navigate from an error page to the source code

Using the `play.editor` configuration option, you can set up Play to add hyperlinks to an error page.  This will link to runtime exceptions thrown when Play is running development mode.

You can easily navigate from error pages to IntelliJ directly into the source code, by using IntelliJ's "remote file" REST API with the built in IntelliJ web server on port 63342.

Enable the following line in `application.conf` to provide hyperlinks:

```
play.editor="http://localhost:63342/api/file/?file=%s&line=%s"
```

You can also set play.editor from `build.sbt`:

```scala
fork := true // required for "sbt run" to pick up javaOptions

javaOptions += "-Dplay.editor=http://localhost:63342/api/file/?file=%s&line=%s"
```

or set the PLAY_EDITOR environment variable:

```
PLAY_EDITOR="http://localhost:63342/api/file/?file=%s&line=%s"
```

## Netbeans

### Generate Configuration

Play does not have native [Netbeans](https://netbeans.org/) project generation support at this time, but there is a Scala plugin for NetBeans which can help with both Scala language and SBT:

<https://github.com/dcaoyuan/nbscala>

There is also a SBT plugin to create Netbeans project definition:

<https://github.com/dcaoyuan/nbsbt>

## ENSIME

### Install ENSIME

Follow the installation instructions at <https://github.com/ensime/ensime-emacs>.

### Generate configuration

Edit your project/plugins.sbt file, and add the following line (you should first check <https://github.com/ensime/ensime-sbt> for the latest version of the plugin):

```scala
addSbtPlugin("org.ensime" % "sbt-ensime" % "2.0.1")
```

Start SBT:

```bash
$ sbt
```

Enter 'ensimeConfig' at the [sbt shell](http://www.scala-sbt.org/0.13/docs/Howto-Interactive-Mode.html). The plugin should generate a .ensime file in the root of your Play project.

```bash
[[play-scala-seed] $ ensimeConfig
[info] ENSIME update.
...
[info] ENSIME processing root (play-scala-seed)
```

### Start ENSIME

From Emacs, execute M-x ensime and follow the on-screen instructions.

That's all there is to it. You should now get type-checking, completion, etc. for your Play project. Note, if you add new library dependencies to your play project, you'll need to re-run "ensimeConfig" and re-launch ENSIME.

### More Information

Check out the ENSIME README at <https://github.com/ensime/ensime-emacs>. If you have questions, post them in the ensime group at <https://groups.google.com/forum/?fromgroups=#!forum/ensime>.

## All Scala Plugins if needed

1. Eclipse Scala IDE: <http://scala-ide.org/>
2. NetBeans Scala Plugin: <https://github.com/dcaoyuan/nbscala>
3. IntelliJ IDEA Scala Plugin: <https://blog.jetbrains.com/scala/>
4. ENSIME - Scala IDE Mode for Emacs: <https://github.com/ensime/ensime-emacs>
