<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Java Configuration API Migration

The class `play.Configuration` was deprecated in favor of using [Typesafe Config](https://github.com/typesafehub/config) directly. So, instead of using `play.Configuration` you must now use [`com.typesafe.config.Config`](https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html). For example:

Before:
```java
import play.Configuration;
public class Foo {
    private final Configuration configuration;

    @javax.inject.Inject
    public Foo(Configuration configuration) {
        this.configuration = configuration;
    }
}
```

After:
```java
import com.typesafe.config.Config;

public class Foo {
    private final Config config;

    @javax.inject.Inject
    public Foo(Config config) {
        this.config = config;
    }
}
```

## Config values should always be defined

The main difference between the `Config` and `play.Configuration` APIs is how to handle default values. [Typesafe Config advocates](https://github.com/typesafehub/config/tree/v1.3.1#how-to-handle-defaults) that all configuration keys must be declared in your `.conf` files, including the default values.

Play itself is using `reference.conf` files to declare default values for all the possible configurations. To avoid the hassle of handling missing values, you can do the same if you are distributing a library. When the configuration is read, the `application.conf` files are layered on top of the `reference.conf` configuration. For example:

Before (`configuration` is `play.Configuration`):
```java
// Here we have the default values inside the code, which is not the idiomatic way when using Typesafe Config.
Long timeout = configuration.getMilliseconds("my.service.timeout", 5000); // 5 seconds
```

After:
```
# This is declared in `conf/reference.conf`.
my.service.timeout = 5 seconds
```

And you can eventually override the value in your `application.conf` file:

```
# This will override the value declared in reference.conf
my.service.timeout = 10 seconds
```

This is especially useful when creating modules, since your module can provide reference values that are easy to override. Your Java code will then look like:

```java
Long timeout = config.getDuration("my.service.timeout", TimeUnit.MILLISECONDS);
```

where `config` is your `com.typesafe.config.Config` instance.

## Manually checking values

If you don't want or if you cannot have default values for some reason, you can use [`Config.hasPath`](https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html#hasPath-java.lang.String-) or [`Config.hasPathOrNull`](https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html#hasPathOrNull-java.lang.String-) to check if the value is configured before accessing it. This is a better option if the configuration is required but you can't provide a reference (default) value:

```java
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class EmailServerConfig {

    private static final String SERVER_ADDRESS_KEY = "my.smtp.server.address";

    private final Config config;

    @javax.inject.Inject
    public EmailServerConfig(Config config) {
        this.config = config;
    }

    // The relevant code is here. First use `hasPath` to check if the configuration
    // exists and, if not, throw an exception.
    public String getSmtpAddress() {
        if (config.hasPath(SERVER_ADDRESS_KEY)) {
            return config.getString(SERVER_ADDRESS_KEY);
        } else {
            throw new ConfigException.Missing(SERVER_ADDRESS_KEY);
        }
    }
}
```
