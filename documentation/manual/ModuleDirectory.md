<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play modules

Play uses public modules to augment built-in functionality.  

To create your own public module or to migrate from a `play.api.Plugin`, please see [[ScalaPlayModules]] or [[JavaPlayModules]].

## API hosting

### swagger-play
* **Website:** <https://github.com/swagger-api/swagger-play>
* **Short description:** Generate a Swagger API spec from your Play routes file and Swagger annotations

### iheartradio/play-swagger
* **Website:** <https://github.com/iheartradio/play-swagger>
* **Short description:** Write a Swagger spec in your routes file

### zalando/play-swagger
* **Website:** <https://github.com/zalando/play-swagger>
* **Short description:** Generate Play code from a Swagger spec


## Assets

### play2-sprites
* **Website:** <https://github.com/koofr/play2-sprites/>
* **Short description:** play2-sprites is an sbt plugin that generates sprites from images.

### Sass Plugin
* **Website:** <https://github.com/jlitola/play-sass>
* **Short description:** Asset handling for [Sass](http://sass-lang.com/) files

### Typescript Plugin
* **Website:** <https://github.com/ArpNetworking/sbt-typescript>
* **Short description:** A plugin for SBT that uses sbt-web to compile typescript resources

## Authentication (Login & Registration) and Authorization (Restricted Access)

### Silhouette (Scala)

* **Website:** <http://silhouette.mohiva.com/>
* **Documentation:** <http://silhouette.mohiva.com/docs/>
* **Short description:** An authentication library that supports several authentication methods, including OAuth1, OAuth2, OpenID, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes.

### Deadbolt 2 Plugin

* **Website (docs, sample):** <https://github.com/schaloner/deadbolt-2>
* **Short description:** Deadbolt is an authorisation mechanism for defining access rights to certain controller methods or parts of a view using a simple AND/OR/NOT syntax

### Play-pac4j (Java and Scala)

* **Website:** <https://github.com/pac4j/play-pac4j>
* **Documentation:** <https://github.com/pac4j/play-pac4j/blob/master/README.md>
* **Short description:** Play client in Scala and Java which supports OAuth/CAS/OpenID/HTTP authentication and user profile retrieval

### Authentication and Authorization module (Scala)

* **Website:** <https://github.com/t2v/play20-auth>
* **Documentation(en):** <https://github.com/t2v/play20-auth/blob/master/README.md>
* **Documentation(ja):** <https://github.com/t2v/play20-auth/blob/master/README.ja.md>
* **Short description** This module provides an authentication and authorization way

### Play! Authenticate (Java)

* **Website:** <https://joscha.github.io/play-authenticate/>
* **Documentation:** <https://github.com/joscha/play-authenticate/blob/master/README.md>
* **Short description:** A highly customizable authentication module for Play

### SecureSocial (Java and Scala)

* **Website:** <http://securesocial.ws/>
* **Short description:** An authentication module supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes.


## Datastore

### Flyway plugin

* **Website:** <https://github.com/flyway/flyway-play>
* **Documentation:** <https://github.com/flyway/flyway-play/blob/master/README.md>
* **Short Description:** Supports database migration with Flyway.

### MongoDB Jongo Plugin (Java)
* **Website (docs, sample):** <https://github.com/alexanderjarvis/play-jongo>
* **Short description:** Provides managed MongoDB access and object mapping using [Jongo](http://jongo.org/)

### MongoDB ReactiveMongo Plugin (Scala)
* **Website (docs, sample):** <http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html>
* **Short description:** Provides a Play 2.x module for ReactiveMongo, asynchronous and reactive driver for MongoDB.

### Play-Slick
* **Website (docs, sample):** <https://github.com/freekh/play-slick>
* **Short description:** This plugin makes Slick a first-class citizen of Play.

### Redis Plugin  (Java and Scala)
* **Website (docs, sample):** <https://github.com/typesafehub/play-plugins>
* **Short description:** Provides a redis based cache implementation, also lets you use Redis specific APIs

### ScalikeJDBC Plugin (Scala)

* **Website:** <https://github.com/scalikejdbc/scalikejdbc-play-support>
* **Short description:** Provides yet another database access API for Play



## Deployment

### WAR Module

* **Website:** <https://github.com/dlecan/play2-war-plugin>
* **Documentation:** <https://github.com/dlecan/play2-war-plugin/blob/develop/README.md>
* **Short description:** Allow to package Play! 2.x applications into standard WAR packages.



## Localization

### FolderMessages plugin

* **Website:** <https://github.com/germanosin/play-foldermessages>
* **Short Description:** Allows you to split localization messages files into separate manageable files.

### JsMessages

* **Website:** <https://github.com/julienrf/play-jsmessages>
* **Short description:** Allows to compute localized messages on client side.

### Messages Compiler Plugin (Scala)

* **Website:** <https://github.com/tegonal/play-messagescompiler>
* **Documentation:** <https://github.com/tegonal/play-messagescompiler/blob/master/readme.md>
* **Short description:** Provides type safety for the project's messages.



## Performance

### Google's HTML Compressor (Java and Scala)
* **Website:** <https://github.com/mohiva/play-html-compressor>
* **Documentation:** <https://github.com/mohiva/play-html-compressor/blob/master/README.md>
* **Short description:** Google's HTML Compressor for Play 2.

### Memcached Plugin

* **Website:** <https://github.com/mumoshu/play2-memcached>
* **Short description:** Provides a memcached based cache implementation



## Templates and View

### Google Closure Template Plugin
* **Website (docs, sample):** [https://github.com/gawkermedia/play2-closure](https://github.com/gawkermedia/play2-closure)
* **Short description:** Provides support for Google Closure Templates

### HTML5 Tags module (Java and Scala)
* **Website:** <https://github.com/loicdescotte/Play2-HTML5Tags>
* **Documentation:** <https://github.com/loicdescotte/Play2-HTML5Tags/blob/master/README.md>
* **Short description:** These tags add client side validation capabilities, based on model constraints (e.g required, email pattern, max|min length...) and specific input fields (date, telephone number, url...) to Play templates

### PDF module (Java)

* **Website:** <https://github.com/innoveit/play2-pdf>
* **Documentation:** <https://github.com/innoveit/play2-pdf/blob/master/README.md>
* **Short description** Generate PDF output from HTML templates

### Play-Bootstrap (Java and Scala)
* **Website:** <https://adrianhurt.github.io/play-bootstrap/>
* **Repository:** <https://github.com/adrianhurt/play-bootstrap>
* **Short description:** A library for Bootstrap that gives you an out-of-the-box solution with a set of input helpers and field constructors.

### Play Dok

* **Website:** <https://go.fudok.com/en/>
* **Documentation:** <https://github.com/cchantep/play-dok/>
* **Short description:** Library to integrate Fukdok PDF templating service with your Play application.

### Thymeleaf module (Scala)
* **Website:** <https://github.com/dmitraver/scala-play-thymeleaf-plugin>
* **Documentation:** <https://github.com/dmitraver/scala-play-thymeleaf-plugin/blob/master/README.md>
* **Short description:** Allows to use [Thymeleaf](http://www.thymeleaf.org/) template engine as an alternative
to Twirl



## Utilities

### Emailer Plugin  (Java and Scala)
* **Website (docs, sample):** <https://github.com/playframework/play-mailer>
* **Short description:** Provides an emailer based on apache commons-email

### Geolocation (Java)

* **Website:** <https://edulify.github.io/play-geolocation-module.edulify.com/>
* **Documentation:** <https://github.com/edulify/play-geolocation-module.edulify.com/blob/master/README.md>
* **Short description:** Module to retrieve Geolocation data based on IP.

### JSONP filter

* **Website:** <https://github.com/julienrf/play-jsonp-filter>
* **Short description:** Enables JSONP on your existing HTTP API.

### Sitemap Generator (Java)

* **Website:** <https://edulify.github.io/play-sitemap-module.edulify.com/>
* **Documentation:** <https://github.com/edulify/play-sitemap-module.edulify.com/blob/master/README.md>
* **Short description:** Automatic [sitemaps](http://www.sitemaps.org/) generator for Play



## Cloud services

### Amazon SES module (Scala)

* **Website:** <https://github.com/Rhinofly/play-mailer>
* **Documentation:** <https://github.com/Rhinofly/play-mailer/blob/master/README.md>
* **Short description:** SES (Simple Email Service) API wrapper for Play

### Amazon S3 module (Scala)

* **Website:** <https://github.com/Rhinofly/play-s3>
* **Documentation:** <https://github.com/Rhinofly/play-s3/blob/master/README.md>
* **Short description:** S3 (Simple Storage Service) API wrapper for Play

### Pusher
* **Website:** <https://pusher.com/>
* **Documentation:** <https://github.com/tindr/Play2Pusher>
* **Short description:** Easily interact with the Pusher Service within your Play application.
