<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# What's new in Play 3.0

Play 3.0 marks a new era for the Play Framework. Since late 2021, the project has undergone several major changes.

1. The project is now entirely driven by the community, and fully committed to Open Source.
2. The project transitioned from Lightbend Inc. to a core team of dedicated individuals, as detailed in [our sponsorship page](https://www.playframework.com/sponsors).
3. Play has decided to use [Apache Pekko](https://pekko.apache.org/) under the hood, instead of Akka.

To emphasize all of these key changes, we have decided to mark the transition with a major version change - from 2 to 3. 

In case you are unfamiliar with Pekko, here's what you should know about the project - and why we decided to migrate. Pekko represents a community fork of Akka 2.6 and Akka HTTP 10.2. If you want to know more about Apache Pekko and the motivation to switch to it in Play 3.0, read [["How Play Deals with Akka’s License Change"|General#How-Play-Deals-with-Akkas-License-Change]]. Be aware, like described on the linked page, that you can still continue using Akka and Akka HTTP with [[Play 2.9|Highlights29]]. It's also recommended to familiarize yourself with the Framework's [[End-of-life (EOL) dates|General#End-of-life-(EOL)-Dates]].

Play 3.0 is nearly identical to Play 2.9, and continues to offer support for the latest Java LTS versions and Scala 3. Play 2.9 and 3.0 will offer the same features and receive parallel maintenance, benefiting from identical enhancements and bug fixes. Therefore, almost everything written in the [[Play 2.9 Highlights page|Highlights29]] also applies to Play 3.0. Please read through it to get familiar with what Play 2.9 and Play 3.0 have to offer. Play 3.0 differs from Play 2.9 in only two ways:

* It switches from **Akka and Akka HTTP to [Apache Pekko](https://pekko.apache.org/) and [Apache Pekko HTTP](https://pekko.apache.org/docs/pekko-http/1.0/)**
* **It migrates the `groupId`** from `com.typesafe.play` to `org.playframework`

Please take a look at the [[Play 3.0 Migration Guide|Migration30]] which goes into more detail on those two topics and explains which steps are necessary to migrate your application to Play 3.0.

> Last but not least, we want to express our heartfelt gratitude to all our [Premium sponsors and all Individuals](https://www.playframework.com/#sponsors-backers), current and past ones, whose generous contributions make it possible for us to continue the development of the Play Framework.
> **This release would never have happened without your support!**

On a side note, we would also like to bring to your attention that we are currently in search of an additional Premium Sponsor. If your company is financially equipped for such a role, we would greatly appreciate it if you could reach out to us. Further details can be found [here](https://www.playframework.com/sponsors).
