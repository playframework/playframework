# Play 2.0 current status

Play 2.0 beta is a preview of the next major version of the Play framework, which integrates a brand new build system and an awesome asynchronous architecture, all with native Java and Scala support.

You can start writing Play 2.0 applications right now with the beta version. It will give you an almost complete preview of the Play 2.0 experience: including the native Scala support, the new possibilities provided by the sbt integration, and the new controller/action API.

Play 2.0 beta is not, however, ready to run production applications. 

## What’s missing?

We have worked hard to reach this beta version and are halfway to the final 2.0 version. The following features are the most important items we will be working on now.

### Support for more content types out of the box

Currently we only support parsing classical URL form-encoded HTTP requests. We will add out-of-the-box support for major content types like JSON, XML and file uploads.

### Asynchronous support

Play 2.0 is built from the ground up with reactiveness and optimised resource consumption in mind. We will add a public API for these features to make asynchronous reactive responses, streaming and WebSocket programming really simple.

### Testing

We have basic [[specs | http://code.google.com/p/specs/]] and [[JUnit | http://www.junit.org/]] integration for now. We will add a better API for testing your web applications, either with unit tests or integration tests based on the [[Selenium WebDriver | http://seleniumhq.org/projects/webdriver/]].

### External HTTP libraries

As in Play 1.x, Play 2.0 will provide out of the box integration with a powerful Web Service client, as well as OpenID and OAuth support.

### IDE integration

Currently there is no simple way to integrate your Play 2.0 project in your favorite IDE - you must configure it yourself (or install an sbt plugin like [[sbt-idea | https://github.com/mpeltonen/sbt-idea]]). But we will be adding the cool 'eclipsify', 'netbeansify' and 'idealize' tasks to the Play console.

### WAR support

We also plan to add WAR deployment support via Servlet 3.0.

### And of course, everything that makes a great release

… including more validators, binders, better documentation, tutorials and awesome sample applications.