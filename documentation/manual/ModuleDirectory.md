<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Play modules

Play uses public modules to augment built-in functionality.  

To create your own public module or to migrate from a `play.api.Plugin`, please see [[ScalaPlayModules]] or [[JavaPlayModules]].

## API hosting

### iheartradio/play-swagger

- **Website:** <https://github.com/iheartradio/play-swagger>
- **Short description:** Write a Swagger spec in your routes file

## Assets

### Typescript Plugin
* **Website:** <https://github.com/ArpNetworking/sbt-typescript>
* **Short description:** A plugin for sbt that uses sbt-web to compile typescript resources


## Authentication (Login & Registration) and Authorization (Restricted Access)

### Silhouette (Scala)

* **Website:** <https://silhouette.readme.io/>
* **Documentation:** <https://silhouette.readme.io/docs>
* **Short description:** An authentication library that supports several authentication methods, including OAuth1, OAuth2, OpenID, CAS, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes.

### Play-pac4j (Java and Scala)

* **Website:** <https://github.com/pac4j/play-pac4j>
* **Documentation:** <https://github.com/pac4j/play-pac4j/blob/master/README.md>
* **Short description:** Play client in Scala and Java which supports OAuth/CAS/OpenID/HTTP authentication and user profile retrieval

### Play! Authenticate (Java)

* **Website:** <https://joscha.github.io/play-authenticate/>
* **Documentation:** <https://github.com/joscha/play-authenticate/blob/master/README.md>
* **Short description:** A highly customizable authentication module for Play


## Datastore

### Flyway plugin

* **Website:** <https://github.com/flyway/flyway-play>
* **Documentation:** <https://github.com/flyway/flyway-play/blob/master/README.md>
* **Short Description:** Supports database migration with Flyway.

### MongoDB Morphia Plugin (Java)
* **Website (docs, sample):** <https://github.com/morellik/play-morphia>
* **Short description:** Provides managed MongoDB access and object mapping using [Morphia](https://morphia.dev/)

### MongoDB ReactiveMongo Plugin (Scala)
* **Website (docs, sample):** <http://reactivemongo.org/releases/0.1x/documentation/tutorial/play.html>
* **Short description:** Provides a Play 2.x module for ReactiveMongo, asynchronous and reactive driver for MongoDB.

### Play-Slick
* **Website (docs, sample):** <https://github.com/playframework/play-slick>
* **Short description:** This plugin makes Slick a first-class citizen of Play.

### ScalikeJDBC Plugin (Scala)

* **Website:** <https://github.com/scalikejdbc/scalikejdbc-play-support>
* **Short description:** Provides yet another database access API for Play

### Redis Cache Plugin (Java and Scala)

* **Website:** <https://github.com/KarelCemus/play-redis>
* **Short description:** Provides both blocking and asynchronous redis based cache implementation. It implements common Play's CacheApi for both Java and Scala plus provides a few more Scala APIs implementing various Redis commands including the support of collections.


## Page Rendering

### Play Pagelets
* **Website:** <https://github.com/splink/pagelets>
* **Short Description:** A Module for the Play Framework to build resilient and modular Play applications in an elegant and concise manner.
* **Seed project:** <https://github.com/splink/pagelets-seed>


### JsMessages

* **Website:** <https://github.com/julienrf/play-jsmessages>
* **Short description:** Allows to compute localized messages on client side. Play 2.7 support.


## Performance

### Google's HTML Compressor (Java and Scala)
* **Website:** <https://github.com/fkoehler/play-html-compressor>
* **Documentation:** <https://github.com/fkoehler/play-html-compressor/blob/master/README.md>
* **Short description:** Google's HTML Compressor for Play 2.

### Memcached Plugin

* **Website:** <https://github.com/mumoshu/play2-memcached>
* **Short description:** Provides a memcached based cache implementation. Support up to play 2.6.

## Task Schedulers

### Akka Quartz Scheduler

* **Website**: <https://github.com/enragedginger/akka-quartz-scheduler>
* **Documentation**: <https://github.com/enragedginger/akka-quartz-scheduler/blob/master/README.md>
* **Short description**: Quartz Extension and utilities for cron-style scheduling in Akka

## Settings

### Remote Configuration
* **Website:** <https://github.com/play-rconf>
* **Short description:** Loads and apply configuration items (keys & files) from remote providers like etcd, consul, DynamoDB...

## Templates and View

### Google Closure Template Plugin
* **Website (docs, sample):** [https://github.com/gawkermedia/play2-closure](https://github.com/gawkermedia/play2-closure)
* **Short description:** Provides support for Google Closure Templates. Supports Play 2.4.

### Scalate
* **Website:** <https://github.com/scalate/play-scalate>
* **Documentation:** <https://scalate.github.io/scalate/documentation/index.html>
* **Short description:** Alternatives to Twirl HTML template support for Jade (like Haml), Mustache, Scaml (also like Haml), SSP (like Velocity), and Scuery (CSS3 selector language)

### PDF module (Java)

* **Website:** <https://github.com/innoveit/play2-pdf>
* **Documentation:** <https://github.com/innoveit/play2-pdf/blob/master/README.md>
* **Short description** Generate PDF output from HTML templates

### PlayFOP (Java and Scala)

* **Website (live demo, user guide, other docs):** <https://www.dmanchester.com/playfop>
* **Repository:** <https://github.com/dmanchester/playfop>
* **Short description:** A library for creating PDFs, images, and other types of output in Play applications. Accepts XSL-FO that an application has generated and processes it with [Apache FOP](https://xmlgraphics.apache.org/fop/).

### Play-Bootstrap (Java and Scala)
* **Website:** <https://adrianhurt.github.io/play-bootstrap/>
* **Repository:** <https://github.com/adrianhurt/play-bootstrap>
* **Short description:** A library for Bootstrap that gives you an out-of-the-box solution with a set of input helpers and field constructors.


### Play Form

* **Website:** <https://github.com/plippe/play-form>
* **Short description:** A module to submit forms with commonly unsupported browser methods like `PUT`, `PATCH`, and `DELETE`.

## Utilities

### Emailer Plugin  (Java and Scala)
* **Website (docs, sample):** <https://github.com/playframework/play-mailer>
* **Short description:** Provides an emailer based on apache commons-email

### play-guard (Scala)

* **Website:** <https://github.com/sief/play-guard/>
* **Documentation:** <https://github.com/sief/play-guard/blob/master/README.md>
* **Short description:** Play2 module for blocking and throttling abusive requests


## Cloud services

### Benji (Scala)

* **Website:** <https://github.com/zengularity/benji>
* **Documentation:** <https://github.com/zengularity/benji/tree/master/examples>
* **Short description:** A reactive module for the Benji library, providing an Object storage DSL (AWS/Ceph S3, Google Cloud Storage).
