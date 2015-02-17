<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# H2 database

The H2 in memory database is very convenient for development because your evolutions are run from scratch when play is restarted.  If you are using anorm you probably need it to closely mimic your planned production database.  To tell h2 that you want to mimic a particular database you add a parameter to the database url in your application.conf file, for example:

```
db.default.url="jdbc:h2:mem:play;MODE=MYSQL"
```

## Target databases

<table>
<tr>
<tr><td>MySql</td><td>MODE=MYSQL</td>
<td><ul><li>H2 does not have a uuid() function. You can use random_uuid() instead.  Or insert the following line into your 1.sql file: <pre><code>CREATE ALIAS UUID FOR 
"org.h2.value.ValueUuid.getNewRandom";</code></pre></li>  

<li>Text comparison in MySQL is case insensitive by default, while in H2 it is case sensitive (as in most other databases). H2 does support case insensitive text comparison, but it needs to be set separately, using SET IGNORECASE TRUE. This affects comparison using =, LIKE, REGEXP.</li></td></tr>
<tr><td>DB2</td><td>
MODE=DB2</td><td></td></tr>
<tr><td>Derby</td><td>
MODE=DERBY</td><td></td></tr>
<tr><td>HSQLDB</td><td>
MODE=HSQLDB</td><td></td></tr>
<tr><td>MS SQL</td><td>
MODE=MSSQLServer</td><td></td></tr>
<tr><td>Oracle</td><td>
MODE=Oracle</td><td></td></tr>
<tr><td>PostgreSQL</td><td>
MODE=PostgreSQL</td><td></td></tr>
</table>

## Prevent in memory DB reset

H2 drops your database if there no connections.  You probably don't want this to happen.  To prevent this add `DB_CLOSE_DELAY=-1` to the url (use a semicolon as a separator) eg: `jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1`

## H2 Browser

You can browse the contents of your database by typing `h2-browser` at the play console.  An SQL browser will run in your web browser.

## H2 Documentation

More H2 documentation is available [from their web site](http://www.h2database.com/html/features.html)
