<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# What's new in Play 2.9

This page highlights the new features of Play 2.9. If you want to learn about the changes you need to make when you migrate to Play 2.9, check out the [[Play 2.9 Migration Guide|Migration29]].

## TBA

TBA

## Other additions

### Deferred body parsing

By default body parsing takes place _before_ actions defined via action composition are processed. This order can now be changed. See more details on why and how to do that in [[Java|JavaActionsComposition#Action-composition-in-interaction-with-body-parsing]] and [[Scala|ScalaActionsComposition#Action-composition-in-interaction-with-body-parsing]] documentation.

### The location of the evolution scripts can now be configured

Using the new `play.evolutions[.db.default].path` config it is now possible the store evolution scripts in a custom location within a Play project or even outside the project's root folder by referencing the location with an absolute or relative path. All details can be found in the [[Evolutions documentation|Evolutions#Location-of-the-evolution-scripts]].

### Variable substitution in evolutions scripts

You can now define placeholders in your evolutions scripts which will be replaced with their substitutions, defined in `application.conf`:

```
play.evolutions.db.default.substitutions.mappings = {
  table = "users"
  name = "John"
}
```

An evolution script like

```sql
INSERT INTO $evolutions{{{table}}}(username) VALUES ('$evolutions{{{name}}}');
```

will now become

```sql
INSERT INTO users(username) VALUES ('John');
```

at the moment when evolutions get applied.

> The evolutions meta table will contain the raw sql script, _without_ placeholders replaced.
>
> The meta table is called `play_evolutions` by default. This naming can be changed by setting the config `play.evolutions.db.default.metaTable` since this release.

Variable substitution is case insensitive, therefore `$evolutions{{{NAME}}}` is the same as `$evolutions{{{name}}}`.

You can also change the prefix and suffix of the placeholder syntax:

```
# Change syntax to @{...}
play.evolutions.db.default.substitutions.prefix = "@{"
play.evolutions.db.default.substitutions.suffix = "}"
```

The evolution module also comes with support for escaping, for cases where variables should not be substituted. This escaping mechanism is enabled by default. To disable it you need to set:

```
play.evolutions.db.default.substitutions.escapeEnabled = false
```

If enabled, the syntax `!$evolutions{{{...}}}` can be used to escape variable substitution. For example:

```
INSERT INTO notes(comment) VALUES ('!$evolutions{{{comment}}}');
```

will not be replaced with its substitution, but instead will become

```
INSERT INTO notes(comment) VALUES ('$evolutions{{{comment}}}');
```

in the final sql.

> This escape mechanism will be applied to all `!$evolutions{{{...}}}` placeholders, no matter if a mapping for a variable is defined in the `substitutions.mappings` config or not.

### IP filter added

A new filter was added to restrict access by black-/whitelisting IP addresses.

Please see [[the IP filter page|IPFilter]] for more details.

### Play-Configuration project as standalone library

Play's `play.api.Configuration` class, which is a wrapper around [Typesafe Config](https://github.com/lightbend/config), can now be used as standalone library.
We tried to keep its footprint as small as possible: Besides the Typesafe Config, it therefore only depends on `slf4j-api` for logging and on Play's `play-exceptions` project which only contains two exceptions classes that are also needed by Play itself and the Play SBT Plugin.
To use the library you can add it to any Scala project by adding it to your `build.sbt`:

```scala
libraryDependencies += "com.typesafe.play" %% "play-configuration" % "<PLAY_VERSION>"
```
