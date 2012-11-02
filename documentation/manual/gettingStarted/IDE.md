# Setting up your preferred IDE

Working with Play is easy. You don’t even need a sophisticated IDE, because Play compiles and refreshes the modifications you make to your source files automatically, so you can easily work using a simple text editor.

However, using a modern Java or Scala IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging.

## Eclipse

### Generate configuration

Play provides a command to simplify Eclipse configuration. To transform a Play application into a working Eclipse project, use the `eclipse` command:

without the source jars:

```
[My first application] $ eclipse
```

if you want to grab the available source jars (this will take longer and it's possible a few sources might be missing):

```
[My first application] $ eclipse with-source=true
```

> Note if you are using sub-projects with aggregate, you would need to set `skipParents` appropriately:

```
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys

object ApplicationBuild extends Build {

  override def settings = super.settings ++ Seq(
      EclipseKeys.skipParents in ThisBuild := false
  )

  ...
 
}
```
or from the play console, type:

``` 
[My first application] $ eclipsify skip-parents=false
```

> Also, if you did not want to trigger a compilation before running `eclipse`, then just add the following to your settings:

```
EclipsePlugin.EclipseKeys.preTasks := Seq()
```

You then need to import the application into your Workspace with the **File/Import/General/Existing project…** menu (compile your project first).

[[images/eclipse.png]] 

You can also start your application with `play debug run` and then you can use the Connect JPDA launcher using **Debug As** to start a debugging session at any time. Stopping the debugging session will not stop the server.


> **Tip**: You can run your application using `~run` to enable direct compilation on file change. This way scala template files are auto discovered when you create a new template in `view` and auto compiled when the file changes. If you use normal `run` then you have to hit `Refresh` on your browser each time.

If you make any important changes to your application, such as changing the classpath, use `eclipse` again to regenerate the configuration files.

> **Tip**: Do not commit Eclipse configuration files when you work in a team!

The generated configuration files contain absolute references to your framework installation. These are specific to your own installation. When you work in a team, each developer must keep his Eclipse configuration files private.

## IntelliJ

### Generate configuration

Play provides a command to simplify Intellij IDEA configuration. To transform a Play application into a working IDEA module, use the idea command:

without the source jars:

```
[My first application] $ idea
```

if you want to grab the available source jars (this will take longer and it's possible a few sources might be missing):

```
[My first application] $ idea with-sources
```

This will create the configuration files IntelliJ needs to open your play application as a project. The files are named <project>.iml and <project>-build.iml. The file menu (IntelliJ 11.1 CE) contains the Open Project command.

> Tip: There is an [Intellij IDEA issue](http://devnet.jetbrains.net/thread/433870) regarding building Java based Play2 apps while having the Scala plugin installed. Until it's fixed, the recommended workaround is to disable the Scala plugin.

To debug, first add a debug configuration

- Open Run/Debug Configurations dialog, then click Run -> Edit Configurations
- Add a Remote configuration, then select `Remote`
- Configure it:
    - Set a name
    - Transport: Socket
    - Debugger mode: Attach
    - Host: localhost
    - Port: 9999
    - Select module you imported
- Close dialog - click Apply

Start play in debug mode (in a separate command line console, NOT in IDEA's Play console):

```
$ play debug
```

which should print: 

```
Listening for transport dt_socket at address: 9999
```

Set some breakpoints then start your new debug configuration from within IDEA. The console output should be:

```
Connected to the target VM, address: 'localhost:9999', transport: 'socket'
```

Run the web app by executing the task `run` in the Play console. Finally, browse to `http://localhost:9000`. IntelliJ should stop at your breakpoint.

Alternatively, in order not to run more command prompts, first run "play debug run" in IDEA's Play console, then launch debug configuration.

If you make any important changes to your application, such as changing the classpath, use `idea` again to regenerate the configuration files.


## Netbeans

### Generate Configuration

Play does not have native Netbeans project generation support at this time.  For now you can generate a Netbeans Scala project with the [Netbeans SBT plugin](https://github.com/remeniuk/sbt-netbeans-plugin).

First edit the plugins.sbt file

```
resolvers += {
  "remeniuk repo" at "http://remeniuk.github.com/maven" 
}

libraryDependencies += {
  "org.netbeans" %% "sbt-netbeans-plugin" % "0.1.4"
}
```

Now run

```
$ play netbeans
```


## ENSIME

### Install ENSIME

Follow the installation instructions at http://github.com/aemoncannon/ensime

### Generate configuration

Edit your project/plugins.sbt file, and add the following line (you should first check http://github.com/aemoncannon/ensime-sbt-cmd for the latest version of the plugin):

```
addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.0")
```

Start Play:

```
$ play
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

Check out the ENSIME manual at http://aemoncannon.github.com/ensime/index.html
If you have questions, post them in the ensime group at https://groups.google.com/forum/?fromgroups=#!forum/ensime


## All Scala Plugins if needed

Scala is a newer programming language, so the functionality is provided in plugins rather than in the core IDE.

- Eclipse Scala IDE: http://scala-ide.org/
- NetBeans Scala Plugin: http://java.net/projects/nbscala
- IntelliJ IDEA Scala Plugin: http://confluence.jetbrains.net/display/SCA/Scala+Plugin+for+IntelliJ+IDEA
- IntelliJ IDEA's plugin is under active development, and so using the nightly build may give you additional functionality at the cost of some minor hiccups.
- Nika (11.x) Plugin Repository: http://www.jetbrains.com/idea/plugins/scala-nightly-nika.xml
- Leda (12.x) Plugin Repository: http://www.jetbrains.com/idea/plugins/scala-nightly-leda.xml
- IntelliJ IDEA Play 2.0 plugin (available only for Leda 12.x): http://plugins.intellij.net/plugin/?idea&pluginId=7080
- ENSIME - Scala IDE Mode for Emacs: https://github.com/aemoncannon/ensime
(see below for ENSIME/Play instructions)

&nbsp;

> **Next:** 
>
> – [[Play for Scala developers|ScalaHome]]
> – [[Play for Java developers|JavaHome]]