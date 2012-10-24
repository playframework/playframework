# Contributor Guidelines

Implementation-wise, the following things should be avoided as much as possible:

* public mutable state
* global state
* implicit conversions
* threadLocal
* locks
* casting
* introducing new, heavy external dependencies

## API design

* Play is a Java and Scala framework, make sure your changes are working for both API-s
* Java APIs should go to ```framework/play/src/main/java```, package structure is ```play.myapipackage.xxxx```
* Scala APIs should go to ```framework/play/src/main/scala```, where the package structure is ```play.api.myapipackage```
* Java and Scala APIs should be implemented the following way:
  * implement the core API in scala (```play.api.xxx```)
  * if your component requires life cycle management or needs to be swappable, create a plugin, otherwise skip this step
  * wrap core API for scala users ([example]  (https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/cache/Cache.scala#L69))
  * wrap scala API for java users ([example](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/java/play/cache/Cache.java))
* features are forever, always think about whether a new feature really belongs to the core framework or it should be implemented as a plugin
* if you are in doubt, ask on the mailing list


## Testing and documentation

* each and every public facing method and class need to have a corresponding scaladoc or javadoc with examples, description etc.
* each feature and bug fix requires either a functional test (```framework/integrationtest```) or a spec (```/play/src/test```)
* run Play's integration test suite ```framework/runtests``` before pushing. If a test fails, fix it, do not ignore it.

## source format

* run scalariform-format  before commit

## git commits

* prefer rebase
* bigger changesets