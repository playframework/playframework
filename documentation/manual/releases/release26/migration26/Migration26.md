<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.6 Migration Guide

This is a guide for migrating from Play 2.5 to Play 2.6. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.5 Migration Guide|Migration25]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

## JPA Migration Notes

See [[JPAMigration26]].

### Removed Yaml API

We removed `play.libs.Yaml` since there was no use of it inside of play anymore.
If you still need support for the Play YAML integration you need to add `snakeyaml` in you `build.sbt`:

```
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```
 
And create the following Wrapper in your Code:

```
public class Yaml {

    private final play.Environment environment;

    @Inject
    public Yaml(play.Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Load a Yaml file from the classpath.
     */
    public static Object load(String resourceName) {
        return load(
            environment.resourceAsStream(resourceName),
            environment.classLoader()
        );
    }
    
    /** 
     * Load the specified InputStream as Yaml.
     *
     * @param classloader The classloader to use to instantiate Java objects.
     */
    public static Object load(InputStream is, ClassLoader classloader) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new CustomClassLoaderConstructor(classloader));
        return yaml.load(is);
    }
    
}
```


### Removed Libraries

In order to make the default play distribution a bit smaller we removed some libraries.
The following libraries are no longer dependencies in Play 2.6, so you will need to manually add them to your build if you use them.

#### Joda-Time removal

If you can, you could migrate all occurences to Java8 `java.time`.

If you can't and still need to use Joda-Time in Play Forms and Play-Json you can just add the `play-joda` project:

```
libraryDependencies += "com.typesafe.play" % "play-joda" % "1.0.0"
```

And then import the corresponding Object for Forms:

```
import play.api.data.JodaForms._
```

or for Play-Json

```
import play.api.data.JodaWrites._
import play.api.data.JodaReads._
```

#### Joda-Convert removal

Play had some internal uses of `joda-convert` if you used it in your project you need to add it to your `build.sbt`:

```
libraryDependencies += "org.joda" % "joda-convert" % "1.8.1"
```

#### XercesImpl removal

For XML handling Play used the Xerces XML Library. Since modern JVM are using Xerces as a refernece implementation we removed it.
If your project relies on the external package you can simply add it to your `build.sbt`:

```
libraryDependencies += "xerces" % "xercesImpl" % "2.11.0"
```

#### H2 removal

Prior versions of Play prepackaged the H2 database. But to make the core of Play smaller we removed it.
If you make use of h2 you can add it to your `build.sbt`:

```
libraryDependencies += "com.h2database" % "h2" % "1.4.191"
```

If you only used it in your test you can also just use the `Test` scope:

```
libraryDependencies += "com.h2database" % "h2" % "1.4.191" % Test
```

The [[H2 Browser|Developing-with-the-H2-Database#H2-Browser]] will still work after you added the dependency.

#### snakeyaml removal

Play removed `play.libs.Yaml` and therefore the dependency on `snakeyaml` was dropped.
If you still use it add it to your `build.sbt`:

```
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

### Tomcat-servlet-api removal

Play removed the `tomcat-servlet-api` since it was of no use.

```
libraryDependencies += "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.33"
```

### Akka Migration

The deprecated static methods `play.libs.Akka.system` and `play.api.libs.concurrent.Akka.system` were removed.  Please dependency inject an `ActorSystem` instance for access to the actor system.