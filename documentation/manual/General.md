<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

## How Play Deals with Akka's License Change

You may have heard that in September 2022, [Lightbend Inc.](https://www.lightbend.com/) changed the Akka license model, switching from the Apache 2.0 license to the [Business Source License (BSL) 1.1](https://www.lightbend.com/akka/license).
This change took effect with Akka 2.7 and Akka HTTP 10.4:

- [Announcement by Lightbend](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka) titled _"Why We Are Changing the License for Akka"_
- [Akka 2.7.0 and Akka HTTP 10.4.0](https://akka.io/blog/news/2022/10/26/akka-22.10-released) from October 26th, 2022
- [InfoQ article](https://www.infoq.com/news/2022/09/akka-no-longer-open-source/) titled _"Lightbend Changes Akka License and Is No Longer Open Source"_
- [Akka Pricing](https://www.lightbend.com/akka#pricing) License fees may apply under certain conditions
- [Akka License FAQ](https://www.lightbend.com/akka/license-faq)

Play versions 2.x make use of Akka under the hood and also offers framework users ways to interact with Akka classes and APIs in the Play Framework documentation. Additionally, apart from the Netty backend, the Akka HTTP server backend is available in Play 2.x.

Many people were and are concerned about whether their companies have to pay license fees if Play switches to a newer Akka version that is licensed under the BSL. To address these concerns, Lightbend introduced an ["Additional Use Grant"](https://www.lightbend.com/akka/license) for the Play Framework. This allows the use of Akka within Play 2.x and enables Play Framework users to utilize certain Akka functionality in their source code, including production environments, without being subject to license restrictions. However, both Play Framework users and Premium Sponsors expressed concerns that these exceptions are too narrow. This means that if you use Akka in a Play 2.x application in a way that is not documented in the official Play Framework documentation, you may already be required to obtain a license. It's quite easy to have such use cases fall outside of that exception. For instance, injecting an `ActorSystem` (not intended for WebSockets) would already trigger the application of the new BSL license, subjecting you to the full Akka license model.

Our top priority is to prevent Play Framework users from inadvertently falling under the BSL license scheme without their knowledge and to avoid having the Play Framework depend on non-open-source libraries. Consequently, **[[Play 2.9+|Highlights29]] continues to ship with Akka 2.6 and Akka HTTP 10.2 by default**. <!--These versions are the last purely open-source iterations of Akka.--> If users wish to upgrade to an Akka version that employs the BSL, the decision is theirs to make, and it's a conscious choice where users will be fully aware of the new license model. Our [Play Scala](https://www.playframework.com/documentation/2.9.x/ScalaAkka#Updating-Akka-version) or [Play Java](https://www.playframework.com/documentation/2.9.x/JavaAkka#Updating-Akka-version) update guides provide assistance on how to make this transition.

> **Be aware:** Akka 2.6 / Akka HTTP 10.2 [reached end-of-life (EOL)](https://twitter.com/akkateam/status/1568297981421694976) **in September 2023**.

### Introducing Apache Pekko and Play 3.0

We released [[Play 3.0|Highlights30]] in October 2023. This release replaced Akka and Akka HTTP with Pekko and Pekko HTTP: [Apache Pekko](https://pekko.apache.org/) represents a community fork of Akka 2.6 and Akka HTTP 10.2, developed by highly engaged and motivated individuals, some of whom were formerly part of the Lightbend Akka team. Pekko is undergoing active development. For example, Pekko HTTP has already been ported to Scala 3, a feature that is currently accessible only starting from the BSL-licensed Akka HTTP version 10.4. Also, Pekko has already fixed bugs and security vulnerabilities that have not been addressed in Akka 2.6 and Akka HTTP 10.2 anymore.

Aside from utilizing different frameworks under the hood, [[Play 2.9|Highlights29]] and [[Play 3.0|Highlights30]] offer the same features and receive parallel maintenance, benefiting from identical enhancements and bug fixes.

### Summary of Your Options

- When using Play 2.9: Upgrade to a newer Akka and Akka HTTP release under the BSL licensing using our [Play Scala](https://www.playframework.com/documentation/2.9.x/ScalaAkka#Updating-Akka-version) or [Play Java](https://www.playframework.com/documentation/2.9.x/JavaAkka#Updating-Akka-version) update guides.
- Transition to Play 3.0: It replaces Akka and Akka HTTP with Pekko and Pekko HTTP.
    - This transition doesn't require rewriting your application; you'll only need to replace `akka.*` imports and configuration keys.
- Alternatives when using Play 2.9: Stick with the default Akka and Akka HTTP versions shipped with Play, and choose to:
    - Switch from Akka HTTP to the [[Netty Server Backend|NettyServer]] (keep in mind that Play 2.9 will still use Akka 2.6 in the background).
    - Continue using them while being aware that this could lead to security issues, as they won't receive security patches anymore.

## End-of-life (EOL) Dates

- All versions prior to Play 2.8 have reached their end of life and will no longer receive updates.
- Play 2.8 will reach its end of life on May 31st, 2024. Until that date, Play 2.8 will only receive security upgrades. The following notable libraries will no longer be upgraded automatically (although you can do so manually if desired):
    - Jackson, which Play 2.8 ships with version 2.11.x ([how to upgrade](https://github.com/orgs/playframework/discussions/11222))
    - Logback, which Play 2.8 ships with Version 1.2.x ([how to upgrade](https://github.com/playframework/playframework/issues/11499#issuecomment-1285654119))
    - SLF4J, which Play 2.8 ships with Version 1.7.x ([how to upgrade](https://github.com/playframework/playframework/issues/11499#issuecomment-1285654119))
    - Compatibility with sbt 1.9+ may not work out of the box, but there is [a workaround](https://github.com/playframework/playframework/releases/tag/2.8.19#user-content-sbt19comp).
- Play 3.0 and Play 2.9: Security updates will be provided for 12 months after the release of the subsequent 2.x or 3.x version. However, we will evaluate the duration of Play 2.x line maintenance in September 2024. We intend to release at least one new major Play 2.x version (2.10) with new features and upgraded dependencies before that time. At any point, if we decide not to continue the 2.x line, we will maintain the last 2.x version for an additional 12 months to ensure it continues to receive security updates.

> If you are considering using Play 2.9 (with the BSL-licensed Akka and Akka HTTP), we want to inform you that the Play core team has limited resources. You might want to consider becoming a Premium sponsor to help keep the 2.x line alive. Otherwise, if interest in the 2.x line declines, we may need to discontinue it at some point.
