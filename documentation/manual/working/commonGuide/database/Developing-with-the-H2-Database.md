<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# H2 database

> **Note:** From Play 2.6.x onwards you actually need to include the H2 Dependency on your own. To do this you just need to add the following to your build.sbt:
>
> ```
> libraryDependencies += "com.h2database" % "h2" % "1.4.192"
> ```

The H2 in memory database is very convenient for development because your evolutions are run from scratch when play is restarted.  If you are using Anorm, you probably need it to closely mimic your planned production database.  To tell h2 that you want to mimic a particular database you add a parameter to the database url in your application.conf file, for example:

```
db.default.url="jdbc:h2:mem:play;MODE=MYSQL"
```

## Target databases

<table>
  <tbody>
    <tr>
      <td>MySql</td>
      <td>MODE=MYSQL</td>
      <td>
        <ul>
          <li>H2 does not have a uuid() function. You can use random_uuid() instead. Or insert the following line into your 1.sql file: <pre><code>CREATE ALIAS UUID FOR
"org.h2.value.ValueUuid.getNewRandom";</code></pre>
          </li>
          <li>Text comparison in MySQL is case insensitive by default, while in H2 it is case sensitive (as in most other databases). H2 does support case insensitive text comparison, but it needs to be set separately, using SET IGNORECASE TRUE. This affects comparison using =, LIKE, REGEXP.</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>DB2</td>
      <td>MODE=DB2</td>
      <td></td>
    </tr>
    <tr>
      <td>Derby</td>
      <td>MODE=DERBY</td>
      <td></td>
    </tr>
    <tr>
      <td>HSQLDB</td>
      <td>MODE=HSQLDB</td>
      <td></td>
    </tr>
    <tr>
      <td>MS SQL</td>
      <td>MODE=MSSQLServer</td>
      <td></td>
    </tr>
    <tr>
      <td>Oracle</td>
      <td>MODE=Oracle</td>
      <td></td>
    </tr>
    <tr>
      <td>PostgreSQL</td>
      <td>MODE=PostgreSQL</td>
      <td></td>
     </tr>
  </tbody>
</table>

## Prevent in memory DB reset

H2, by default, drops your in memory database if there are no connections to it anymore.  You probably don't want this to happen.  To prevent this add `DB_CLOSE_DELAY=-1` to the url (use a semicolon as a separator) eg: `jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1`

> **Note:** Play's builtin JDBC Module will automatically add `DB_CLOSE_DELAY=-1`, however if you are using play-slick with evolutions you need to manually add `;DB_CLOSE_DELAY=-1` to your database url, else the evolution will be in a endless loop since the play application will restart after the evolutions are run, so that the applied evolutions will directly be lost.

## Caveats

H2, by default, creates tables with upper case names. Sometimes you don't want this to happen, for example when using H2 with Play evolutions in some compatibility modes. To prevent this add `DATABASE_TO_UPPER=FALSE` to the url (use a semicolon as a separator) eg: `jdbc:h2:mem:play;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE`

## H2 Browser

You can browse the contents of your database by typing `h2-browser` at the [sbt shell](https://www.scala-sbt.org/1.x/docs/Howto-Interactive-Mode.html).  An SQL browser will run in your web browser.

## H2 Documentation

More H2 documentation is available [from their web site](http://www.h2database.com/html/features.html).
