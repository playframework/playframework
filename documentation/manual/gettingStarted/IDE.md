<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Setting up your preferred IDE

Working with Play is easy. You don’t even need a sophisticated IDE, because Play compiles and refreshes the modifications you make to your source files automatically, so you can easily work using a simple text editor.

However, using a modern Java or Scala IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging.

## Eclipse

### Setup sbt-eclipse

Integration with Eclipse requires [sbt-eclipse](https://github.com/sbt/sbt-eclipse). Make sure to always use the [most recent available version](https://github.com/sbt/sbt-eclipse/releases) in your project/plugins.sbt file or follow [sbt-eclipse docs](https://github.com/sbt/sbt-eclipse) to install globally.

@[add-sbt-eclipse-plugin](code/ide.sbt)

You must `compile` your project before running the `eclipse` command. You can force compilation to happen when the `eclipse` command runs by adding the following setting in build.sbt:

@[sbt-eclipse-plugin-preTasks](code/ide.sbt)

If you have Scala sources in your project, you will need to install [Scala IDE](http://scala-ide.org/).

If you do not want to install Scala IDE and have only Java sources in your project, then you can set the following build.sbt (assuming you have no Scala sources):

@[sbt-eclipse-plugin-projectFlavor](code/ide.sbt)

### Generate configuration

After configuring sbt-eclipse, to transform a Play application into a working Eclipse project, use the `eclipse` command:

```bash
[my-first-app] $ eclipse
```

If you want to grab the available source jars (this will take longer and it's possible a few sources might be missing):

```bash
[my-first-app] $ eclipse with-source=true
```

> **Note**: if you are using sub-projects with aggregate, you would need to set `skipParents` appropriately in `build.sbt`:

@[sbt-eclipse-plugin-skipParents](code/ide.sbt)

or from the [sbt shell](https://www.scala-sbt.org/1.x/docs/Howto-Interactive-Mode.html), type:

```bash
[my-first-app] $ eclipse skip-parents=false
```

You then need to import the application into your Workspace with the **File/Import/General/Existing project…** menu (compile your project first).

[[images/eclipse.png]]

To debug, start your application with `sbt -jvm-debug 9999 run` and in Eclipse right-click on the project and select **Debug As**, **Debug Configurations**. In the **Debug Configurations** dialog, right-click on **Remote Java Application** and select **New**. Change **Port** to 9999 and click **Apply**. From now on you can click on **Debug** to connect to the running application. Stopping the debugging session will not stop the server.

If you make any important changes to your application, such as changing the classpath, use `eclipse` again to regenerate the configuration files.

> **Tip**: Do not commit Eclipse configuration files when you work in a team. To make that easier, add the following lines to your `.gitignore` file:
> 
> ```
> /.classpath
> /.project
> /.settings
> ```

The generated configuration files contain absolute references to your framework installation. These are specific to your own installation. When you work in a team, each developer must keep their Eclipse configuration files private.

## IntelliJ IDEA

[Intellij IDEA](https://www.jetbrains.com/idea/) lets you quickly create a Play application without using a command prompt. You don't need to configure anything outside of the IDE, the sbt build tool takes care of downloading appropriate libraries, resolving dependencies and building the project.

Before you start creating a Play application in IntelliJ IDEA, make sure the latest [Scala Plugin](https://www.jetbrains.com/idea/help/creating-and-running-your-scala-application.html) is installed and enabled in IntelliJ IDEA. Even if you don't develop in Scala, it will help with the template engine, resolving the dependencies, and also setting up the project in general.

To create a Play application:

1. Open ***New Project*** wizard, select ***sbt*** under ***Scala*** section and click ***Next***.
2. Enter your project's information and click ***Finish***.

You can also import an existing Play project.

To import a Play project:

1. Open Project wizard, select ***Import Project***.
2. In the window that opens, select a project you want to import and click ***OK***.
3. On the next page of the wizard, select ***Import project from external model*** option, choose ***sbt project*** and click ***Next***.
4. On the next page of the wizard, select additional import options and click ***Finish***.

> **Tip**: you can download and import one of our [starter projects](https://playframework.com/download#starters) or either one of the [example projects](https://github.com/playframework/play-samples).

Check the project's structure, make sure all necessary dependencies are downloaded. You can use code assistance, navigation and on-the-fly code analysis features.

You can run the created application and view the result in the default browser <http://localhost:9000>. To run a Play application:

1. Create a new Run Configuration -- From the main menu, select Run -> Edit Configurations
2. Click on the + to add a new configuration
3. From the list of configurations, choose "sbt Task"
4. In the "tasks" input box, simply put "run"
5. Apply changes and select OK.
6. Now you can choose "Run" from the main Run menu and run your application

You can easily start a debugger session for a Play application using default Run/Debug Configuration settings.

For more detailed information, see the Play Framework tutorial at the following URL:

<https://www.jetbrains.com/help/idea/getting-started-with-play-framework.html>

### Navigate from an error page to the source code

Using the `play.editor` configuration option, you can set up Play to add hyperlinks to an error page.  This will link to runtime exceptions thrown when Play is running development mode.

You can easily navigate from error pages to IntelliJ directly into the source code, by using IntelliJ's "remote file" REST API with the built in IntelliJ web server on port 63342.

Enable the following line in `application.conf` to provide hyperlinks:

```
play.editor="http://localhost:63342/api/file/?file=%s&line=%s"
```

You can also set play.editor from `build.sbt`:

```scala
javaOptions += "-Dplay.editor=http://localhost:63342/api/file/?file=%s&line=%s"
```

or set the PLAY_EDITOR environment variable:

```
PLAY_EDITOR="http://localhost:63342/api/file/?file=%s&line=%s"
```

## NetBeans

### Generate Configuration

Play does not have native [NetBeans](https://netbeans.org/) project generation support at this time, but there is a Scala plugin for NetBeans, which can help with both Scala language and sbt:

<https://github.com/dcaoyuan/nbscala>

There is also a sbt plugin to create NetBeans project definition:

<https://github.com/dcaoyuan/nbsbt>

## Visual Studio Code

[Visual Studio Code](https://code.visualstudio.com/), commonly called VS Code for short, does not have native Scala or Play support. Instead, it has access to the [Metals](https://scalameta.org/metals/) language server. To get started:

1. Go to Extensions (`Ctrl + Shift + X` by default)
2. Search for `Metals` and install `Scala (Metals)` by `Scalameta`
3. Go to any scala repository you have and if detected, Metals will request you to import the build

If you have any issues with working with Metals, ensure you're using a Java version equal to or greater than 11.

For any further help, check out the [Metals](https://scalameta.org/metals/docs#installation) documentation.

## All Scala Plugins if needed

1. Eclipse Scala IDE: <http://scala-ide.org/>
2. NetBeans Scala Plugin: <https://github.com/dcaoyuan/nbscala>
3. IntelliJ IDEA Scala Plugin: <https://blog.jetbrains.com/scala/>
4. ENSIME - Scala IDE Mode for Emacs: <https://github.com/ensime/ensime-emacs>
