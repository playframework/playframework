<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Preface

Before delving into the highlights of this release, we highly recommend familiarizing yourself with the following topics, as they are likely to be relevant to your usage of the Play Framework for your business:

- [[Play 3.0 vs Play 2.9: How Play Deals with Akka’s License Change|General#How-Play-Deals-with-Akkas-License-Change]]
- [[End-of-life (EOL) dates|General#End-of-life-(EOL)-Dates]]

We would also like to bring to your attention that we are currently in search of an additional Premium Sponsor. If your company is financially equipped for such a role, we would greatly appreciate it if you could reach out to us. Further details can be found [here](https://www.playframework.com/sponsors).

> Last but not least, we want to express our heartfelt gratitude to all our [Premium sponsors and all Individuals](https://www.playframework.com/#sponsors-backers), current and past ones, whose generous contributions make it possible for us to continue the development of the Play Framework.
> **This release would never have happened without your support!**

# What's new in Play 2.9

This section highlights the new features of Play 2.9. If you want to learn about the changes you need to make when you migrate to Play 2.9, check out the [[Play 2.9 Migration Guide|Migration29]].

> This page also applies to [Play 3.0](https://www.playframework.com/documentation/latest/Highlights30), which contains the same new features and bug fixes like the simultaneously released Play 2.9. The only difference is that Play 2.9 is built on Akka and Akka HTTP, whereas Play 3.0 is built on Pekko and Pekko HTTP. If you want to continue migrating to Play 3.0 with Pekko after migrating to Play 2.9, refer to the  [Play 3.0 Migration Guide](https://www.playframework.com/documentation/latest/Migration30).

## Scala 3 support

Play 2.9 provides Scala 3 artifacts! You can now use Play with Scala 3.3.1 or newer. Note that Scala versions ranging from 3.0 to 3.2 are not supported. Please be mindful that certain migration steps will be required. For comprehensive guidance, please consult the [[Scala 3 migration guide|Scala3Migration]].

> It's important to emphasize that Play exclusively supports [Scala LTS (Long-Term Support)](https://www.scala-lang.org/blog/2022/08/17/long-term-compatibility-plans.html) versions. As a result, any Scala release between Scala 3.3 LTS and the subsequent LTS version will not be officially supported by Play. However, it might still be feasible to use Play with such Scala versions.

There is no immediate pressure to transition your Play applications to Scala 3, as Scala 2.13 is still being maintained, and we anticipate new [Scala 2.13 releases](https://github.com/scala/scala/releases) for many years to come. Nevertheless, it's wise to start considering the upgrade of your applications in your future roadmaps.

> Depending on your codebase, migrating existing Play applications to Scala 3 can be a substantial task. We strongly recommend that you initially [[migrate|Migration29]] to Play 2.9 while staying on Scala 2.13. This approach ensures that everything functions as intended. Afterward, you can [[make the transition|Scala3Migration]] to Scala 3.

## Java 17 and 21 Support

Play 2.9 is the first major version to offer out-of-the-box support for Java 21 LTS and Java 17 LTS. Although it was already possible to use Java 17 with Play 2.8.15, [this still required some adjustments](https://github.com/playframework/playframework/releases/tag/2.8.15#user-content-java17).

When upgrading to Play 2.9, we strongly recommend considering an upgrade to at least Java 17 LTS. It’s worth noting that in the forthcoming Play 2.x release, which will be version 2.10, we are likely to discontinue support for Java 11. Our decision to make this recommendation is backed by the fact that libraries that Play depends on have already stopped providing Java 11 artifacts. We therefore want to avoid running into security problems in the future because we may be unable to upgrade those dependencies. Additionally, you might be interested in the local benchmarking we conducted before the Play 2.9 release, using the [TechEmpower's Framework Benchmarks suite](https://www.techempower.com/benchmarks/): The results demonstrated improvements in performance with the simple act of upgrading to Java 17, potentially leading to enhanced performance for your application.

> Please be aware that binding an HTTPS port with a self-signed certificate in Java 17 and Java 21 may lead to issues. For more details on this matter, refer to [["Generation of Self-Signed Certificates Fails in Java 17 and Java 21"|Migration29#Generation-of-Self-Signed-Certificates-Fails-in-Java-17-and-Java-21]] in the Play 2.9 Migration Guide.

Play, its standalone modules, samples, and seed projects are all rigorously tested against [OpenJDK Temurin](https://adoptium.net/) versions 11, 17, and 21.

> Play exclusively supports Java LTS versions. Consequently, any Java release between Java 21 LTS and the subsequent LTS will not receive official support from Play, even though it may still be possible to use Play with such a version.

## Scala 2.12, sbt 0.13 and Java 8 Support discontinued

Alongside Scala 2.12, we have opted to discontinue support for Java 8 in this release. This decision was necessitated by the growing number of libraries that Play depends on, which have ceased providing Java 8 artifacts. Additionally, Play 2.9 has finally bid farewell to support for sbt 0.13. While Play is likely to remain compatible with various sbt 1.x versions, our official support is extended to sbt version 1.9.6 or newer. Consequently, we strongly advise maintaining your setup with the [latest available sbt version](https://github.com/sbt/sbt/releases).

## Akka HTTP 10.2

While Play 2.9 continues to utilize Akka 2.6, it has advanced to Akka HTTP 10.2, an upgrade from 10.1. It's important to note that Play 2.9 will maintain these versions without going beyond them. This decision is grounded in the fact that Akka 2.6 and Akka HTTP 10.2 represent the final versions still governed by the Apache License. Subsequent [Akka releases are subject to the BSL](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka), which Play deliberately avoids using. We strongly encourage you to review:

- [[How Play Deals with Akka’s License Change|General#How-Play-Deals-with-Akkas-License-Change]]

## Guice Upgraded to Version 6

The default dependency injection framework used by Play, Guice, has been upgraded to version 6.0.0 (from 4.2.3). Take a look at the [5.0.1](https://github.com/google/guice/wiki/Guice501), [5.1.0](https://github.com/google/guice/wiki/Guice510), and [6.0.0](https://github.com/google/guice/wiki/Guice600) release notes for further details. As indicated in the Guice 6 release notes, you can continue using the `javax.inject` namespace. Alternatively, you have the option to switch to `jakarta.inject` if you prefer.

It's important to note that in the next major release of Play, we will upgrade to [Guice 7](https://github.com/google/guice/wiki/Guice700), which no longer supports `javax.inject`.

## Jackson Upgraded to 2.14

Jackson has received a long-awaited update beyond version 2.11. In Play 2.9, the default Jackson version is 2.14, even though newer versions are already available. This decision is primarily influenced by Akka and the broader compatibility landscape. Neither Akka nor Pekko has yet upgraded to these newer Jackson versions, and we aim to avoid the potential risk of breaking compatibility with these critical components. However, if you desire, you can already upgrade to a newer version in the same way as described [here](https://github.com/orgs/playframework/discussions/11222).

## Transition to Jakarta Persistence

The transition from Java Persistence API to the [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/) now provides support for Hibernate 6 and newer, as well as EclipseLink 3 and newer. Please refer to the corresponding [[migration guide|Migration29#Switched-to-Jakarta-JPA]] for detailed instructions on upgrading Hibernate or EclipseLink to the new namespace.

## Upgraded sbt-web and sbt-js-engine

[sbt-web](https://github.com/sbt/sbt-web) and [sbt-js-engine](https://github.com/sbt/sbt-js-engine) have undergone significant refactoring, leading to several notable improvements:

sbt-web has [removed its dependency](https://github.com/sbt/sbt-web/pull/169) on `Akka`, simplifying both codebases and, as a beneficial side effect, addressing a memory leak. The previously standalone projects [npm](https://github.com/typesafehub/npm) and [js-engine](https://github.com/typesafehub/js-engine) have been integrated into sbt-js-engine.

Starting with version 1.3.0, the version Play ships with, sbt-js-engine attempts to detect the system's `npm` command when using a locally installed js engine that supports Node.js. It will use the system's `npm` instead of the outdated `npm` webjars, which doesn't support newer `npm` commands. This behavior is now the default and can be disabled with `npmPreferSystemInstalledNpm := false`. This setting ensures that the webjar npm is used regardless of the engine in use.

Furthermore, starting from version 1.3.0, it will be possible to specify which `npm` subcommand to use by setting `npmSubcommand := NpmSubcommand.Install` (available options include `Install`, `Ci`, and `Update`, with the latter being the default).

## HikariCP Upgrade

[HikariCP](https://github.com/brettwooldridge/HikariCP), Play's default JDBC connection pool, has been upgraded to version 5. You can review the [changelog](https://github.com/brettwooldridge/HikariCP/blob/dev/CHANGES) for details about this version. This upgrade has allowed us to introduce a new configuration option: `play.db.prototype.hikaricp.keepaliveTime` (or `db.default.hikaricp.keepaliveTime` for a specific database, such as `default`). This configuration accepts a duration value, like `1 minute`.

For more comprehensive information about this setting, please refer to the [HikariCP GitHub README](https://github.com/brettwooldridge/HikariCP).

## Other Additions

### Result Attributes

Similar to request attributes, Play now supports result attributes. These attributes function in the same manner and allow you to store extra information inside result objects for future access. This feature is particularly useful when leveraging action composition or filters. As the methods are the same as those used for request attributes, you can refer to how [[request attributes work|Highlights26#Request-attributes]] and apply a similar approach to result objects.

### Deferred Body Parsing

By default, body parsing occurs _before_ actions defined via action composition are processed. This order can now be altered. For more details on why and how to change this, refer to the [[Java|JavaActionsComposition#Action-composition-in-interaction-with-body-parsing]] and [[Scala|ScalaActionsComposition#Action-composition-in-interaction-with-body-parsing]] documentation.

### Inclusion of `WebSocket` Action Methods in Action Composition

When using Play Java, consider the following controller example that utilizes [[action composition|JavaActionsComposition]]:

```java
@Security.Authenticated
public class HomeController extends Controller {

    @Restrict({ @Group({"admin"}) })
    public WebSocket socket() {
        return WebSocket.Text.acceptOrResult(request -> /* ... */);
    }
}
```

Previously, action composition was not applied when handling `WebSocket`s, such as the `socket()` method shown above. Consequently, annotations like `@Security.Authenticated` and `@Restrict` (from the [Deadbolt 2 library](https://github.com/schaloner/deadbolt-2-java/)) had no effect, and they were never executed. The Java WebSocket documentation suggests using the `acceptOrResult` method to check user permissions and alike.

Starting from Play 2.9, you can now enable the newly introduced configuration option `play.http.actionComposition.includeWebSocketActions` by setting it to `true`. This inclusion of WebSocket action methods in action composition ensures that actions annotated with `@Security.Authenticated` and `@Restrict` are now executed as someone might expect. The advantage with this approach is that you might not need to duplicate authentication or authorization code in the `acceptOrResult` method that is already implemented in the annotation action methods.

### Configurable Evolutions Script Location

The location of evolution scripts can now be customized using the new `play.evolutions[.db.default].path` configuration. This enables the storage of evolution scripts in a specific location within a Play project, or even outside the project's root folder using an absolute or relative path. For comprehensive information, please consult the [[Evolutions documentation|Evolutions#Location-of-the-evolution-scripts]].

### Variable Substitution in Evolutions Scripts

Evolutions scripts now support placeholders that are replaced with their corresponding values as defined in `application.conf`:

```conf
play.evolutions.db.default.substitutions.mappings = {
  table = "users"
  name = "John"
}
```

For instance, an evolution script like:

```sql
INSERT INTO $evolutions{{{table}}}(username) VALUES ('$evolutions{{{name}}}');
```

will be transformed into:

```sql
INSERT INTO users(username) VALUES ('John');
```

during the evolution application process.

> Note: The evolutions meta table retains the raw SQL script, _without_ the placeholders replaced.
>
> The meta table is named `play_evolutions` by default. You can modify this by configuring `play.evolutions.db.default.metaTable`.

Variable substitution is case-insensitive. Thus, `$evolutions{{{NAME}}}` is equivalent to `$evolutions{{{name}}}`.

You can also modify the placeholder syntax's prefix and suffix:

```conf
# Change syntax to @{...}
play.evolutions.db.default.substitutions.prefix = "@{"
play.evolutions.db.default.substitutions.suffix = "}"
```

The evolution module supports an escape mechanism for cases where variables should not be substituted. This mechanism is enabled by default. To disable it, set:

```conf
play.evolutions.db.default.substitutions.escapeEnabled = false
```

When enabled, `!$evolutions{{{...}}}` can be used to escape variable substitution. For instance:

```sql
INSERT INTO notes(comment) VALUES ('!$evolutions{{{comment}}}');
```

will not be replaced with its substitution, but will instead become:

```sql
INSERT INTO notes(comment) VALUES ('$evolutions{{{comment}}}');
```

in the final SQL.

> This escape mechanism applies to all `!$evolutions{{{...}}}` placeholders, regardless of whether a variable mapping is defined in the `substitutions.mappings` configuration or not.

### Custom Naming for Evolution Meta Data Table

It is now possible to rename the `play_evolutions` meta data table for each data source. For instance, if you wish to name the meta table for the `default` data source as `migrations`, set the following configuration:

```conf
play.evolutions.db.default.metaTable = "migrations"
```

If you use a locks table via the `useLocks` configuration, the locks table will also be named the same as your evolution meta data table with a `_lock` postfix (`migrations_lock` instead of `play_evolutions_lock`). Refer to the [[Evolutions|Evolutions]] documentation for more information.

> If you intend to use this feature in existing Play applications, manually rename the meta table(s) before configuring this.

### Allow File Uploads with Empty Body or Empty Filenames

You can now set

```conf
play.http.parser.allowEmptyFile = true
```

to allow empty `multipart/form-data` file uploads, regardless of whether the filename or the file itself is empty. By default, this configuration is set to `false`.

### Request Attribute with Additional Error Information

Within an error handler, you _may_ now access a "http error info" request attribute, if available. This attribute contains supplementary information useful for error handling. Currently, you can obtain details about where the error handler was initially called:

Java
: ```java
request.attrs.getOptional(play.http.HttpErrorHandler.Attrs.HTTP_ERROR_INFO).map(info => info.origin()).orElse("<unknown error origin>")
```

Scala
: ```scala
request.attrs.get(play.api.http.HttpErrorHandler.Attrs.HttpErrorInfo).map(_.origin).getOrElse("<unknown error origin>")
```

Presently, Play 2.9 provides the following values depending on the error handler's origin:

- `csrf-filter` - The error handler was called in CSRF filter code.
- `csp-filter` - The error handler was called in CSP filter code.
- `allowed-hosts-filter` - The error handler was called in Allowed hosts filter code.
- `server-backend` - The handler was called in either the Netty or Akka HTTP server backend.

Third-party modules might introduce their own origins. Additionally, future Play releases may include further error-related information.

### CORS Filter Supports `*` Wildcard

The `allowedOrigins` configuration of the CORS filter now accepts the wildcard `*`, which has a special meaning. If the request origin does not match any other origins in the list, `Access-Control-Allow-Origin: *` will be sent:

```conf
play.filters.cors.allowedOrigins = ["*"]
```

For more details, refer to the [[CORS filter documentation|CorsFilter]].

### New IP Filter

A new filter has been introduced to restrict access by blacklisting or whitelisting IP addresses. Refer to [[the IP filter page|IPFilter]] for detailed information.

### Play-Configuration Project as Standalone Library

Play's `play.api.Configuration` class, which wraps [Typesafe Config](https://github.com/lightbend/config), can now be used as a standalone library. We've aimed to keep its footprint minimal. Apart from Typesafe Config, it only depends on `slf4j-api` for logging and Play's `play-exceptions` project, which includes two exception classes needed by Play itself and the Play SBT Plugin.

To use the library, add it to any Scala project by including it in your `build.sbt`:

```scala
libraryDependencies += "com.typesafe.play" %% "play-configuration" % "<PLAY_VERSION>"
```

### Addition of a UUID PathBindableExtractor

The Routing DSL now includes the capability to bind UUIDs out of the box.

### New Server Backend Configuration Keys

- The configuration key `play.server.akka.terminationTimeout` has been extended to the Netty backend and consequently renamed to `play.server.terminationTimeout`.
- A new configuration key `play.server.waitBeforeTermination` has been introduced, providing a way to work around [Akka HTTP bug #3209](https://github.com/akka/akka-http/issues/3209). This key can also be utilized for the Netty backend.
- For the Netty backend, a new configuration key `play.server.netty.shutdownQuietPeriod` has been added to adjust Netty's quiet period.

For a comprehensive understanding of the functionality of these new configurations, refer to the [[Akka HTTP|SettingsAkkaHttp]] and [[Netty|SettingsNetty]] server backend configuration pages, as well as the documentation on how to "[[Gracefully shutdown the server|Shutdown#Gracefully-shutdown-the-server]]".

## Enhanced Build Infrastructure

On a side note, we want to inform you that we have transitioned all repositories under the GitHub Play Framework organization from Travis CI to GitHub Actions. In our experience, GitHub Actions has proven to be more reliable, significantly faster, and easier to maintain due to its seamless integration with GitHub. We owe a debt of gratitude to [Sergey Morgunov](https://github.com/ihostage) for his exceptional work in facilitating this migration. He invested a considerable amount of time in creating exceptional [reusable GitHub Workflows](https://github.com/playframework/.github/tree/main/.github).

Thanks to [sbt-ci-release](https://github.com/sbt/sbt-ci-release), we have streamlined the process of releasing new versions. Now, we can generate new versions by simply pushing a git tag to GitHub. This eliminates the need to be concerned about potential errors when publishing artifacts locally on developer machines, a scenario that occurred quite frequently in the past.

Switching to helpers, which are now quite common in the Scala developer community, aids us even more in achieving seamless and swift release of new versions with updated libraries. Although these tools are widely recognized, we want to take this opportunity to acknowledge their significance. We extend our heartfelt gratitude to the creators and maintainers of tools like [Scala Steward](https://github.com/scala-steward-org/scala-steward) and [Release Drafter](https://github.com/release-drafter/release-drafter), among others, for their remarkable contributions. Their efforts play a crucial role in enabling us to continue delivering Play.

# Plans for Play 2.10 / Play 3.1

We'd like to provide you with a preliminary overview of what we're planning to include in Play 2.10 / Play 3.1, so you can start considering these changes:

- **Transition to Guice 7**: Embracing Jakarta and discontinuing the `javax.inject` namespace.
- **Removal of Play App Global State**: We aim to eliminate the Play app global state once and for all.
- **Removal of Java 11 Support**: As part of our evolution, Java 11 support will be phased out.
- **Removal of deprecated APIs**: Like always, we will be removing methods and classes marked as deprecated.
- **Upgrade to JUnit 5**: Moving forward with the latest testing framework.
- **Upgrade to Ehcache 3.x**: (Although we recommend Caffeine) for improved caching capabilities.
- **Upgrading to a Newer Jackson Version**: Enhancing JSON processing capabilities.
- **Better Documentation for Onboarding Contributors**: Improved resources to help new contributors understand the Play build setup.
- **Infrastructure Enhancements**: Our release process has come a long way from Play 2.8, but full automation is still a goal. We're also considering migrating our documentation and Play website to GitHub pages. This change would allow automatic publication alongside Play releases, eliminating the need for a dedicated web server. Our medium-term goal is to maximize automation and reduce reliance on third-party services and external hosting, with GitHub being a central hub. Simplifying maintenance and the release process for maintainers is a key objective of this endeavor.

This list, of course, is not exhaustive. You can keep track of our plans through our [GitHub Roadmap](https://github.com/orgs/playframework/projects/3) and the [Play 2.10 milestone](https://github.com/playframework/playframework/milestone/126).
