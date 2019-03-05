<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.8

This page highlights the new features of Play 2.8. If you want to learn about the changes you need to make when you migrate to Play 2.8, check out the [[Play 2.8 Migration Guide|Migration28]].

## Akka 2.6

Play 2.8 brings the latest minor version of Akka. Although Akka 2.6 is binary compatible with 2.5, there are changes in the default configurations and some deprecated features were removed. You can see more details about the changes in [Akka 2.6 migration guide](https://doc.akka.io/docs/akka/2.6.0-M1/project/migration-guide-2.5.x-2.6.x.html).

## Other additions

### Lang cookie max age configuration

It is now possible to configure a max age for language cookie. To do that, add the following to your `application.conf`:

```
play.i18n.langCookieMaxAge = 15 seconds
```

By the default, the configuration is `null` meaning no max age will be set for Lang cookie.
