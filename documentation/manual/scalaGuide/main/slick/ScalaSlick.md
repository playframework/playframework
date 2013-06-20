# Play Slick
It is easy to use Slick in Play. To learn more about Slick continue reading [here](http://slick.typesafe.com/).
To learn more about how to use it with play continue on this page.

The play-slick plugins consists of 2 parts:

DDL schema generation Plugin that works like the Ebean DDL Plugin. Based on config it generates create schema and drop schema SQL commands and writes them to evolutions.
A wrapper DB object that uses the datasources defined in the Play config files. It is there so it is possible to use Slick sessions in the same fashion as you would Anorm JDBC connections.

## Add Play Slick to your project

To add the Play Slick plugin, simply add the jdbc and the playSlick lines to your to your appDependencies like this : 

```scala
val appDependencies = Seq(
  jdbc,
  playSlick
)
```

# DDL plugin

In order to enable DDL schema generation you must specify the packages or classes you want to have in the `application.conf` file:
`slick.default="models.*"`
It follows the same format as the Ebean plugin: `slick.default="models.*"` means all Tables in the models package should be run on the default database.

It is possible to specify individual objects like: `slick.default="models.Users,models.Settings"`

## DB wrapper

The DB wrapper is just a thin wrapper that uses Slicks Database classes with databases in the Play Application. 

This is an example usage:
  
```scala
import play.api.db.slick.Config.driver.simple._

play.api.db.slick.DB.withSession{ implicit session =>
  Users.insert(User("fredrik","ekholdt"))
}
```

Or transactionally:

```scala
import play.api.db.slick.Config.driver.simple._

play.api.db.slick.DB.withTransaction{ implicit session =>
  val list = Query(Users).filter(name === "fredrik")
  val updatedUsers = update(list)
  Users.insertAll(updatedUsers)
}
```


Using `import play.api.db.slick.Config.driver.simple.`_ will import the driver defing with the key `db.default.driver` in application.conf, or the one set by the test helpers in test mode (see test section for more information).

Here is a configuration example for the default database : 

```conf
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.user=sa
db.default.password=""
```


If you need to use more than one database driver per mode (run or test), please read the [[Using multiple drivers | Using-multiple-drivers]].

> **Next:** [[Using multiple drivers | Using-multiple-drivers]]


