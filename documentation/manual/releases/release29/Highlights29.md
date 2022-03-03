<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.9

This page highlights the new features of Play 2.9. If you want to learn about the changes you need to make when you migrate to Play 2.9, check out the [[Play 2.9 Migration Guide|Migration29]].

## TBA

TBA

## Other additions

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
