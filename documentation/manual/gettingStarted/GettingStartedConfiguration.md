# Configuration - Getting Started

The configuration file of a Play application must be defined in `conf/application.conf`. It uses
the [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md).

As well as the `application.conf` file, configuration comes from a couple of other places.

Default settings are loaded from any `reference.conf` files found on the classpath. Most Play JARs include
a `reference.conf` file with default settings. Settings in `application.conf` will override settings
in `reference.conf` files.

For example, the `play-redis` module has a set of standard configurations 
[which can be found here](https://github.com/KarelCemus/play-redis/blob/master/src/main/resources/reference.conf). 
If we define `play.cache.redis.host=example.com` in our `application.conf` this will override the default value of
`play.cache.redis.host=localhost` in the base `reference.conf`

## Overriding or extending `application.conf`

You can create another HOCON file in your `conf` directory called `test.conf` with the following contents.

```HOCON
include "application.conf"

play.cache.redis.host=test.example.cpom
```

This can then be used in the test context by adding this line to your `build.sbt` file:

```sbt
javaOptions in Test ++= Seq("-Dconfig.file=conf/test.conf")
```

The override hierarchy will now look like `reference.conf` -> `application.conf` -> `test.conf`.

## Learn more 

- [[Configuration file syntax and features|ConfigFile]]
