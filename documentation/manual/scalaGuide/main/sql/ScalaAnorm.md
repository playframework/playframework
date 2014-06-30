<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Anorm, simple SQL data access

Play includes a simple data access layer called Anorm that uses plain SQL to interact with the database and provides an API to parse and transform the resulting datasets.

**Anorm is Not an Object Relational Mapper**

> In the following documentation, we will use the [MySQL world sample database](http://dev.mysql.com/doc/index-other.html). 
> 
> If you want to enable it for your application, follow the MySQL website instructions, and configure it as explained [[on the Scala database page | ScalaDatabase]].

## Overview

It can feel strange to return to plain old SQL to access an SQL database these days, especially for Java developers accustomed to using a high-level Object Relational Mapper like Hibernate to completely hide this aspect.

Although we agree that these tools are almost required in Java, we think that they are not needed at all when you have the power of a higher-level programming language like Scala. On the contrary, they will quickly become counter-productive.

#### Using JDBC is a pain, but we provide a better API

We agree that using the JDBC API directly is tedious, particularly in Java. You have to deal with checked exceptions everywhere and iterate over and over around the ResultSet to transform this raw dataset into your own data structure.

We provide a simpler API for JDBC; using Scala you don’t need to bother with exceptions, and transforming data is really easy with a functional language. In fact, the goal of the Play Scala SQL access layer is to provide several APIs to effectively transform JDBC data into other Scala structures.

#### You don’t need another DSL to access relational databases

SQL is already the best DSL for accessing relational databases. We don’t need to invent something new. Moreover the SQL syntax and features can differ from one database vendor to another. 

If you try to abstract this point with another proprietary SQL like DSL you will have to deal with several ‘dialects’ dedicated for each vendor (like Hibernate ones), and limit yourself by not using a particular database’s interesting features.

Play will sometimes provide you with pre-filled SQL statements, but the idea is not to hide the fact that we use SQL under the hood. Play just saves typing a bunch of characters for trivial queries, and you can always fall back to plain old SQL.

#### A type safe DSL to generate SQL is a mistake

Some argue that a type safe DSL is better since all your queries are checked by the compiler. Unfortunately the compiler checks your queries based on a meta-model definition that you often write yourself by ‘mapping’ your data structure to the database schema. 

There are no guarantees that this meta-model is correct. Even if the compiler says that your code and your queries are correctly typed, it can still miserably fail at runtime because of a mismatch in your actual database definition.

#### Take Control of your SQL code

Object Relational Mapping works well for trivial cases, but when you have to deal with complex schemas or existing databases, you will spend most of your time fighting with your ORM to make it generate the SQL queries you want.

Writing SQL queries yourself can be tedious for a simple ‘Hello World’ application, but for any real-life application, you will eventually save time and simplify your code by taking full control of your SQL code.

## Add Anorm to your project

You will need to add Anorm and jdbc plugin to your dependencies : 

```scala
libraryDependencies ++= Seq(
  jdbc,
  anorm
)
```

## Executing SQL queries

To start you need to learn how to execute SQL queries.

First, import `anorm._`, and then simply use the `SQL` object to create queries. You need a `Connection` to run a query, and you can retrieve one from the `play.api.db.DB` helper:

```scala
import anorm._ 
import play.api.db.DB

DB.withConnection { implicit c =>
  val result: Boolean = SQL("Select 1").execute()    
} 
```

The `execute()` method returns a Boolean value indicating whether the execution was successful.

To execute an update, you can use `executeUpdate()`, which returns the number of rows updated.

```scala
val result: Int = SQL("delete from City where id = 99").executeUpdate()
```

If you are inserting data that has an auto-generated `Long` primary key, you can call `executeInsert()`. If you have more than one generated key, or it is not a Long, `executeInsert` can be passed a `ResultSetParser` to return the correct key.

```scala
val id: Option[Long] = 
  SQL("insert into City(name, country) values ({name}, {country})")
  .on('name -> "Cambridge", 'country -> "New Zealand").executeInsert()
```
Since Scala supports multi-line strings, feel free to use them for complex SQL statements:

```scala
val sqlQuery = SQL(
  """
    select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = 'FRA';
  """
)
```

If your SQL query needs dynamic parameters, you can declare placeholders like `{name}` in the query string, and later assign a value to them:

```scala
SQL(
  """
    select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = {countryCode};
  """
).on("countryCode" -> "FRA")
```

In case several columns are found with same name in query result, for example columns named `code` in both `Country` and `CountryLanguage` tables, there can be ambiguity. By default a mapping like following one will use the last column:

```scala
import anorm.{ SQL, SqlParser }

val code: String = SQL(
  """
    select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = {countryCode};
  """)
  .on("countryCode" -> "FRA").as(SqlParser.str("code").single)
```

If `Country.Code` is 'First' and `CountryLanguage` is 'Second', then in previous example `code` value will be 'Second'. Ambiguity can be resolved using qualified column name, with table name:

```scala
import anorm.{ SQL, SqlParser }

val code: String = SQL(
  """
    select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = {countryCode};
  """)
  .on("countryCode" -> "FRA").as(SqlParser.str("Country.code").single)
// code == "First"
```

Columns can also be specified by position, rather than name:

```scala
// Parsing column by name or position
val parser = 
  SqlParser(str("name") ~ float(3) /* third column as float */ map {
    case name ~ f => (name -> f)
  }

val product: (String, Float) = SQL("SELECT * FROM prod WHERE id = {id}").
  on('id -> "p").as(parser.single)
```

`java.util.UUID` can be used as parameter, in which case its string value is passed to statement.

### Using multi-value parameter

Anorm parameter can be multi-value, like a sequence of string.
In such case, values will be prepared to be passed to JDBC.

```scala
// With default formatting (", " as separator)
SQL("SELECT * FROM Test WHERE cat IN ({categories})").
  on('categories -> Seq("a", "b", "c")
// -> SELECT * FROM Test WHERE cat IN ('a', 'b', 'c')

// With custom formatting
import anorm.SeqParameter
SQL("SELECT * FROM Test t WHERE {categories}").
  on('categories -> SeqParameter(
    values = Seq("a", "b", "c"), separator = " OR ", 
    pre = "EXISTS (SELECT NULL FROM j WHERE t.id=j.id AND name=",
    post = ")"))
/* ->
SELECT * FROM Test t WHERE 
EXISTS (SELECT NULL FROM j WHERE t.id=j.id AND name='a') 
OR EXISTS (SELECT NULL FROM j WHERE t.id=j.id AND name='b') 
OR EXISTS (SELECT NULL FROM j WHERE t.id=j.id AND name='c')
*/
```

### Edge cases

Passing anything different from string or symbol as parameter name is now deprecated. For backward compatibility, you can activate `anorm.features.parameterWithUntypedName`.

```scala
import anorm.features.parameterWithUntypedName // activate

val untyped: Any = "name" // deprecated
SQL("SELECT * FROM Country WHERE {p}").on(untyped -> "val")
```

Type of parameter value should be visible, to be properly set on SQL statement.
Using value as `Any`, explicitly or due to erasure, leads to compilation error `No implicit view available from Any => anorm.ParameterValue`.

```scala
// Wrong #1
val p: Any = "strAsAny"
SQL("SELECT * FROM test WHERE id={id}").
  on('id -> p) // Erroneous - No conversion Any => ParameterValue

// Right #1
val p = "strAsString"
SQL("SELECT * FROM test WHERE id={id}").on('id -> p)

// Wrong #2
val ps = Seq("a", "b", 3) // inferred as Seq[Any]
SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").
  on('a -> ps(0), // ps(0) - No conversion Any => ParameterValue
    'b -> ps(1), 
    'c -> ps(2))

// Right #2
val ps = Seq[anorm.ParameterValue]("a", "b", 3) // Seq[ParameterValue]
SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").
  on('a -> ps(0), 'b -> ps(1), 'c -> ps(2))

// Wrong #3
val ts = Seq( // Seq[(String -> Any)] due to _2
  "a" -> "1", "b" -> "2", "c" -> 3)

val nps: Seq[NamedParameter] = ts map { t => 
  val p: NamedParameter = t; p
  // Erroneous - no conversion (String,Any) => NamedParameter
}

SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").on(nps :_*) 

// Right #3
val nps = Seq[NamedParameter]( // Tuples as NamedParameter before Any
  "a" -> "1", "b" -> "2", "c" -> 3)
SQL("SELECT * FROM test WHERE (a={a} AND b={b}) OR c={c}").
  on(nps: _*) // Fail - no conversion (String,Any) => NamedParameter
```

For backward compatibility, you can activate such unsafe parameter conversion, 
accepting untyped `Any` value, with `anorm.features.anyToStatement`.

```scala
import anorm.features.anyToStatement

val d = new java.util.Date()
val params: Seq[NamedParameter] = Seq("mod" -> d, "id" -> "idv")
// Values as Any as heterogenous

SQL("UPDATE item SET last_modified = {mod} WHERE id = {id}").on(params:_*)
```

It's not recommanded because moreover hiding implicit resolution issues, as untyped it could lead to runtime conversion error, with values are passed on statement using `setObject`.
In previous example, `java.util.Date` is accepted as parameter but would with most databases raise error (as it's not valid JDBC type).

### SQL queries using String Interpolation

Since Scala 2.10 supports custom String Interpolation there is also a 1-step alternative to `SQL(queryString).on(params)` seen before. You can abbreviate the code as: 

```scala
val name = "Cambridge"
val country = "New Zealand"

SQL"insert into City(name, country) values ($name, $country)")
```

It also supports multi-line string and inline expresions:

```scala
val lang = "French"
val population = 10000000
val margin = 500000

val code: String = SQL"""
  select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where l.Language = $lang and c.Population >= ${population - margin}
    order by c.Population desc limit 1"""
  .as(SqlParser.str("Country.code").single)
```

This feature tries to make faster, more concise and easier to read the way to retrieve data in Anorm. Please, feel free to use it wherever you see a combination of `SQL().on()` functions (or even an only `SQL()` without parameters).

## Retrieving data using the Stream API

The first way to access the results of a select query is to use the Stream API.

When you call `apply()` on any SQL statement, you will receive a lazy `Stream` of `Row` instances, where each row can be seen as a dictionary:

```scala
// Create an SQL query
val selectCountries = SQL("Select * from Country")
 
// Transform the resulting Stream[Row] to a List[(String,String)]
val countries = selectCountries().map(row => 
  row[String]("code") -> row[String]("name")
).toList
```

In the following example we will count the number of `Country` entries in the database, so the result set will be a single row with a single column:

```scala
// First retrieve the first row
val firstRow = SQL("Select count(*) as c from Country").apply().head
 
// Next get the content of the 'c' column as Long
val countryCount = firstRow[Long]("c")
```

## Using Pattern Matching

You can also use Pattern Matching to match and extract the `Row` content. In this case the column name doesn’t matter. Only the order and the type of the parameters is used to match.

The following example transforms each row to the correct Scala type:

```scala
case class SmallCountry(name:String) 
case class BigCountry(name:String) 
case class France
 
val countries = SQL("Select name,population from Country")().collect {
  case Row("France", _) => France()
  case Row(name:String, pop:Int) if(pop > 1000000) => BigCountry(name)
  case Row(name:String, _) => SmallCountry(name)      
}
```

Note that since `collect(…)` ignores the cases where the partial function isn’t defined, it allows your code to safely ignore rows that you don’t expect.

## Using for-comprehension

Row parser can be defined as for-comprehension, working with SQL result type. It can be useful when working with lot of column, possibly to work around case class limit.

```scala
import anorm.SqlParser.{ str, int }

val parser = for {
  a <- str("colA")
  b <- int("colB")
} yield (a -> b)

val parsed: (String, Int) = SELECT("SELECT * FROM Test").as(parser.single)
```

## Retrieving data along with execution context

Moreover data, query execution involves context information like SQL warnings that may be raised (and may be fatal or not), especially when working with stored SQL procedure.

Way to get context information along with query data is to use `executeQuery()`:

```scala
import anorm.SqlQueryResult

val res: SqlQueryResult = SQL("EXEC stored_proc {code}").
  on('code -> code).executeQuery()

// Check execution context (there warnings) before going on
val str: Option[String] =
  res.statementWarning match {
    case Some(warning) =>
      warning.printStackTrace()
      None

    case _ => res.as(scalar[String].singleOpt) // go on row parsing
  }
```

## Special data types

### Clobs

CLOBs/TEXTs can be extracted as so:

```scala
SQL("Select name,summary from Country")().map {
  case Row(name: String, summary: java.sql.Clob) => name -> summary
}
```

Here we specifically chose to use `map`, as we want an exception if the row isn't in the format we expect.

### Binary

Extracting binary data is similarly possible:

```scala
SQL("Select name,image from Country")().map {
  case Row(name: String, image: Array[Byte]) => name -> image
}
```

### Database interoperability

Note that different databases will return different data types in the Row. For instance, an SQL 'smallint' is returned as a Short by `org.h2.Driver` and an Integer by `org.postgresql.Driver`. A solution to this is to simply write separate case statements for each database (i.e. one for development and one for production).

Anorm provides common mappings for Scala types from JDBC datatypes.

When needed, it's possible to customize such mappings, for example if underlying DB doesn't support boolean datatype and returns integer instead. To do so, you have to provide a new implicit conversion for `Column[T]`, where `T` is the target Scala type:

```scala
import anorm.Column

// Custom conversion from JDBC column to Boolean
implicit def columnToBoolean: Column[Boolean] = 
  Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool) // Provided-default case
      case bit: Int      => Right(bit == 1) // Custom conversion
      case _             => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Boolean for column $qualified"))
    }
  }
```

Custom or specific DB conversion for parameter can also be provided:

```
import java.sql.PreparedStatement
import anorm.ToStatement

// Custom conversion to statement for type T
implicit def customToStatement: ToStatement[T] = new ToStatement[T] {
  def set(statement: PreparedStatement, i: Int, value: T): Unit =
    ??? // Sets |value| on |statement|
}
```

If involved type accept `null` value, it must be appropriately handled in conversion. Even if accepted by type, when `null` must be refused for parameter conversion, marker trait `NotNullGuard` can be used: `new ToStatement[T] with NotNullGuard { /* ... */ }`.

For DB specific parameter, it can be explicitly passed as opaque value.
In this case at your own risk, `setObject` will be used on statement.

```scala
val anyVal: Any = myVal
SQL("UPDATE t SET v = {opaque}").on('opaque -> anorm.Object(anyVal))
```

## Dealing with Nullable columns

If a column can contain `Null` values in the database schema, you need to manipulate it as an `Option` type.

For example, the `indepYear` of the `Country` table is nullable, so you need to match it as `Option[Int]`:

```scala
SQL("Select name,indepYear from Country")().collect {
  case Row(name:String, Some(year:Int)) => name -> year
}
```

If you try to match this column as `Int` it won’t be able to parse `Null` values. Suppose you try to retrieve the column content as `Int` directly from the dictionary:

```scala
SQL("Select name,indepYear from Country")().map { row =>
  row[String]("name") -> row[Int]("indepYear")
}
```

This will produce an `UnexpectedNullableFound(COUNTRY.INDEPYEAR)` exception if it encounters a null value, so you need to map it properly to an `Option[Int]`, as:

```scala
SQL("Select name,indepYear from Country")().map { row =>
  row[String]("name") -> row[Option[Int]]("indepYear")
}
```

This is also true for the parser API, as we will see next.

## Using the Parser API

You can use the parser API to create generic and reusable parsers that can parse the result of any select query.

> **Note:** This is really useful, since most queries in a web application will return similar data sets. For example, if you have defined a parser able to parse a `Country` from a result set, and another `Language` parser, you can then easily compose them to parse both Country and Language from a join query.
>
> First you need to `import anorm.SqlParser._`

### Getting a single result

First you need a `RowParser`, i.e. a parser able to parse one row to a Scala value. For example we can define a parser to transform a single column result set row, to a Scala `Long`:

```scala
val rowParser = scalar[Long]
```

Then we have to transform it into a `ResultSetParser`. Here we will create a parser that parse a single row:

```scala
val rsParser = scalar[Long].single
```

So this parser will parse a result set to return a `Long`. It is useful to parse to result produced by a simple SQL `select count` query:

```scala
val count: Long = 
  SQL("select count(*) from Country").as(scalar[Long].single)
```


### Getting a single optional result

Let's say you want to retrieve the country_id from the country name, but the query might return null. We'll use the singleOpt parser :

```scala
val countryId: Option[Long] = 
  SQL("select country_id from Country C where C.country='France'")
  .as(scalar[Long].singleOpt)
```

### Getting a more complex result

Let’s write a more complicated parser:

`str("name") ~ int("population")`, will create a `RowParser` able to parse a row containing a String `name` column and an Integer `population` column. Then we can create a `ResultSetParser` that will parse as many rows of this kind as it can, using `*`: 

```scala
val populations: List[String~Int] = {
  SQL("select * from Country").as( str("name") ~ int("population") * ) 
}
```

As you see, this query’s result type is `List[String~Int]` - a list of country name and population items.

You can also rewrite the same code as:

```scala
val result: List[String~Int] = {
  SQL("select * from Country")
  .as(get[String]("name") ~ get[Int]("population") *)
}
```

Now what about the `String~Int` type? This is an **Anorm** type that is not really convenient to use outside of your database access code. You would rather have a simple tuple `(String, Int)` instead. You can use the `map` function on a `RowParser` to transform its result to a more convenient type:

```scala
val parser = str("name") ~ int("population") map { case n~p => (n,p) }
```

> **Note:** We created a tuple `(String,Int)` here, but there is nothing stopping you from transforming the `RowParser` result to any other type, such as a custom case class.

Now, because transforming `A ~ B ~ C` types to `(A, B, C)` is a common task, we provide a `flatten` function that does exactly that. So you finally write:

```scala
val result: List[(String, Int)] = 
  SQL("select * from Country").as(parser.*)
```

If list should not be empty, `parser.+` can be used instead of `parser.*`.

### A more complicated example

Now let’s try with a more complicated example. How to parse the result of the following query to retrieve the country name and all spoken languages for a country code?

```
select c.name, l.language from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = 'FRA'
```

Let’s start by parsing all rows as a `List[(String,String)]` (a list of name,language tuple):

```scala
var p: ResultSetParser[List[(String,String)]] = {
  str("name") ~ str("language") map(flatten) *
}
```

Now we get this kind of result:

```scala
List(
  ("France", "Arabic"), 
  ("France", "French"), 
  ("France", "Italian"), 
  ("France", "Portuguese"), 
  ("France", "Spanish"), 
  ("France", "Turkish")
)
```

We can then use the Scala collection API, to transform it to the expected result:

```scala
case class SpokenLanguages(country:String, languages:Seq[String])

languages.headOption.map { f =>
  SpokenLanguages(f._1, languages.map(_._2))
}
```

Finally, we get this convenient function:

```scala
case class SpokenLanguages(country:String, languages:Seq[String])

def spokenLanguages(countryCode: String): Option[SpokenLanguages] = {
  val languages: List[(String, String)] = SQL(
    """
      select c.name, l.language from Country c 
      join CountryLanguage l on l.CountryCode = c.Code 
      where c.code = {code};
    """
  )
  .on("code" -> countryCode)
  .as(str("name") ~ str("language") map(flatten) *)

  languages.headOption.map { f =>
    SpokenLanguages(f._1, languages.map(_._2))
  }
}
```

To continue, let’s complicate our example to separate the official language from the others:

```scala
case class SpokenLanguages(
  country:String, 
  officialLanguage: Option[String], 
  otherLanguages:Seq[String]
)

def spokenLanguages(countryCode: String): Option[SpokenLanguages] = {
  val languages: List[(String, String, Boolean)] = SQL(
    """
      select * from Country c 
      join CountryLanguage l on l.CountryCode = c.Code 
      where c.code = {code};
    """
  )
  .on("code" -> countryCode)
  .as {
    str("name") ~ str("language") ~ str("isOfficial") map {
      case n~l~"T" => (n,l,true)
      case n~l~"F" => (n,l,false)
    } *
  }

  languages.headOption.map { f =>
    SpokenLanguages(
      f._1, 
      languages.find(_._3).map(_._2),
      languages.filterNot(_._3).map(_._2)
    )
  }
}
```

If you try this on the MySQL world sample database, you will get:

```
$ spokenLanguages("FRA")
> Some(
    SpokenLanguages(France,Some(French),List(
        Arabic, Italian, Portuguese, Spanish, Turkish
    ))
)
```

## Type compatibility

As already seen in this documentation, Anorm provides builtins JDBC parsing for various JVM types.

Following table describes which JDBC numeric types (getters on `java.sql.ResultSet`, first column) can be parsed to which Java/Scala types (e.g. integer column can be read as double value).

↓JDBC / JVM➞           | BigDecimal<sup>1</sup> | BigInteger<sup>2</sup> | Boolean | Byte | Double | Float | Int | Long | Short
---------------------- | ---------------------- | ---------------------- | ------- | ---- | ------ | ----- | --- | ---- | -----
BigDecimal<sup>1</sup> | Yes                    | No                     | No      | No   | Yes    | No    | No  | No   | No
BigInteger<sup>2</sup> | No                     | Yes                    | No      | No   | Yes    | Yes   | No  | No   | No
Boolean                | No                     | No                     | Yes     | No   | No     | No    | No  | No   | No
Byte                   | No                     | No                     | No      | Yes  | Yes    | Yes   | No  | No   | Yes
Double                 | Yes                    | No                     | Yes     | No   | Yes    | No    | No  | No   | No
Float                  | No                     | No                     | No      | No   | Yes    | Yes   | No  | No   | No
Int                    | No                     | Yes                    | Yes     | No   | Yes    | Yes   | Yes | Yes  | No
Long                   | Yes                    | Yes                    | No      | No   | No     | No    | No  | Yes  | No
Short                  | No                     | No                     | No      | Yes  | Yes    | Yes   | No  | No   | Yes

- 1. Types `java.math.BigDecimal` and `scala.math.BigDecimal`.
- 2. Types `java.math.BigInteger` and `scala.math.BigInt`.

Second table shows mapping for other supported types (texts, dates, ...).

↓JDBC / JVM➞      | Char | Date | String | UUID<sup>3</sup>
----------------- | ---- | ---- | ------ | ----------------
Clob              | Yes  | No   | Yes    | No
Date              | No   | Yes  | No     | No
Long              | No   | Yes  | No     | No
String            | Yes  | No   | Yes    | No
UUID              | No   | No   | No     | Yes

- 3. Type `java.util.UUID`.

Optional column can be parsed as `Option[T]`, as soon as `T` is supported.

> **Next:** [[Integrating with other database access libraries | ScalaDatabaseOthers]]
