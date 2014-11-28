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

If you are inserting data that has an auto-generated `Long` primary key, you can call `executeInsert()`.

```scala
val id: Option[Long] = 
  SQL("insert into City(name, country) values ({name}, {country})")
  .on('name -> "Cambridge", 'country -> "New Zealand").executeInsert()
```

When key generated on insertion is not a single `Long`, `executeInsert` can be passed a `ResultSetParser` to return the correct key.

```scala
import anorm.SqlParser.str

val id: List[String] = 
  SQL("insert into City(name, country) values ({name}, {country})")
  .on('name -> "Cambridge", 'country -> "New Zealand")
  .executeInsert(str.+) // insertion returns a list of at least one string keys
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

You can also use string interpolation to pass parameters (see details thereafter).

In case several columns are found with same name in query result, for example columns named `code` in both `Country` and `CountryLanguage` tables, there can be ambiguity. By default a mapping like following one will use the last column:

```scala
import anorm.{ SQL, SqlParser }

val code: String = SQL(
  """
    select * from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = {countryCode}
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
    where c.code = {countryCode}
  """)
  .on("countryCode" -> "FRA").as(SqlParser.str("Country.code").single)
// code == "First"
```

When a column is aliased, typically using SQL `AS`, its value can also be resolved. Following example parses column with `country_lang` alias.

```scala
import anorm.{ SQL, SqlParser }

val lang: String = SQL(
  """
    select l.language AS country_lang from Country c 
    join CountryLanguage l on l.CountryCode = c.Code 
    where c.code = {countryCode}
  """).on("countryCode" -> "FRA").
    as(SqlParser.str("country_lang").single)
```

Columns can also be specified by position, rather than name:

```scala
import anorm.SqlParser.{ str, float }
// Parsing column by name or position
val parser = 
  str("name") ~ float(3) /* third column as float */ map {
    case name ~ f => (name -> f)
  }

val product: (String, Float) = SQL("SELECT * FROM prod WHERE id = {id}").
  on('id -> "p").as(parser.single)
```

`java.util.UUID` can be used as parameter, in which case its string value is passed to statement.

### SQL queries using String Interpolation

Since Scala 2.10 supports custom String Interpolation there is also a 1-step alternative to `SQL(queryString).on(params)` seen before. You can abbreviate the code as: 

```scala
val name = "Cambridge"
val country = "New Zealand"

SQL"insert into City(name, country) values ($name, $country)"
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

## Streaming results

Query results can be processed row per row, not having all loaded in memory.

In the following example we will count the number of country rows.

```scala
val countryCount: Either[List[Throwable], Long] = 
  SQL"Select count(*) as c from Country".fold(0L) { (c, _) => c + 1 }
```

> In previous example, either it's the successful `Long` result (right), or the list of errors (left).

Result can also be partially processed:

```scala
val books: Either[List[Throwable], List[String]] = 
  SQL("Select name from Books").foldWhile(List[String]()) { (list, row) => 
    if (list.size == 100) (list -> false) // stop with `list`
    else (list := row[String]("name")) -> true // continue with one more name
  }
```

It's possible to use a custom streaming:

```scala
import anorm.{ Cursor, Row }

@annotation.tailrec
def go(c: Option[Cursor], l: List[String]): List[String] = c match {
  case Some(cursor) => {
    if (l.size == 100) l // custom limit, partial processing
    else {
      go(cursor.next, l :+ cursor.row[String]("name"))
    }
  }
  case _ => l
}

val books: Either[List[Throwable], List[String]] = 
  SQL("Select name from Books").withResult(go(_, List.empty[String]))
```

The parsing API can be used with streaming, using `RowParser` on each cursor `.row`. The previous example can be updated with row parser.

```scala
import scala.util.{ Try, Success => TrySuccess, Failure }

// bookParser: anorm.RowParser[Book]

@annotation.tailrec
def go(c: Option[Cursor], l: List[Book]): Try[List[Book]] = c match {
  case Some(cursor) => {
    if (l.size == 100) l // custom limit, partial processing
    else {
      val parsed: Try[Book] = cursor.row.as(bookParser)

      parsed match {
        case TrySuccess(book) => // book successfully parsed from row
          go(cursor.next, l :+ book)
        case Failure(f) => /* fails to parse a book */ Failure(f)
      }
    }
  }
  case _ => l
}

val books: Either[List[Throwable], Try[List[Book]]] = 
  SQL("Select name from Books").withResult(go(_, List.empty[Book]))

books match {
  case Left(streamingErrors) => ???
  case Right(Failure(parsingError)) => ???
  case Right(TrySuccess(listOfBooks)) => ???
}
```

### Multi-value support

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

On purpose multi-value parameter must strictly be declared with one of supported types (`List`, 'Seq`, `Set`, `SortedSet`, `Stream`, `Vector`  and `SeqParameter`). Value of a subtype must be passed as parameter with supported:

```scala
val seq = IndexedSeq("a", "b", "c")
// seq is instance of Seq with inferred type IndexedSeq[String]

// Wrong
SQL"SELECT * FROM Test WHERE cat in ($seq)"
// Erroneous - No parameter conversion for IndexedSeq[T]

// Right
SQL"SELECT * FROM Test WHERE cat in (${seq: Seq[String]})"

// Right
val param: Seq[String] = seq
SQL"SELECT * FROM Test WHERE cat in ($param)"
```

In case parameter type is JDBC array (`java.sql.Array`), its value can be passed as `Array[T]`, as long as element type `T` is a supported one.

```scala
val arr = Array("fr", "en", "ja")
SQL"UPDATE Test SET langs = $arr".execute()
```

A column can also be multi-value if its type is JDBC array (`java.sql.Array`), then it can be mapped to either array or list (`Array[T]` or `List[T]`), provided type of element (`T`) is also supported in column mapping.

```scala
import anorm.SQL
import anorm.SqlParser.{ scalar, * }

// array and element parser
import anorm.Column.{ columnToArray, stringToArray }

val res: List[Array[String]] =
  SQL("SELECT str_arr FROM tbl").as(scalar[Array[String]].*)
```

> Convenient parsing functions is also provided for arrays with `SqlParser.array[T](...)` and `SqlParser.list[T](...)`.

### Batch update

When you need to execute SQL statement several times with different arguments, batch query can be used (e.g. to execute a batch of insertions).

```scala
import anorm.BatchSql

val batch = BatchSql(
  "INSERT INTO books(title, author) VALUES({title}, {author}", 
  Seq(Seq[NamedParameter](
    "title" -> "Play 2 for Scala", "author" -> Peter Hilton"),
    Seq[NamedParameter]("title" -> "Learning Play! Framework 2",
      "author" -> "Andy Petrella")))

val res: Array[Int] = batch.execute() // array of update count
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

## Using Pattern Matching

You can also use Pattern Matching to match and extract the `Row` content. In this case the column name doesn’t matter. Only the order and the type of the parameters is used to match.

The following example transforms each row to the correct Scala type:

```scala
case class SmallCountry(name:String) 
case class BigCountry(name:String) 
case class France
 
val countries = SQL("SELECT name,population FROM Country WHERE id = {i}").
  on("i" -> "id").map({
    case Row("France", _) => France()
    case Row(name:String, pop:Int) if(pop > 1000000) => BigCountry(name)
    case Row(name:String, _) => SmallCountry(name)      
  }).list
```

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

## Working with optional/nullable columns

If a column in database can contain `Null` values, you need to manipulate it as an `Option` type.

For example, the `indepYear` of the `Country` table is nullable, so you need to match it as `Option[Int]`:

```scala
case class Info(name: String, year: Option[Int])

val parser = str("name") ~ get[Option[Int]]("indepYear") map {
  case n ~ y => Info(n, y)
}

val res: List[Info] = SQL("Select name,indepYear from Country").as(parser.*)
```

If you try to match this column as `Int` it won’t be able to parse `Null` values. Suppose you try to retrieve the column content as `Int` directly from the dictionary:

```scala
SQL("Select name,indepYear from Country")().map { row =>
  row[String]("name") -> row[Int]("indepYear")
}
```

This will produce an `UnexpectedNullableFound(COUNTRY.INDEPYEAR)` exception if it encounters a null value, so you need to map it properly to an `Option[Int]`.

> Nullable parameters are also passed `Option[T]`, `T` being parameter base type (see *Parameters* section thereafter).

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

If expected single result is optional (0 or 1 row), then `scalar` parser can be combined with `singleOpt`:

```scala
val name: Option[String] =
  SQL"SELECT name From Country WHERE code = $code" as scalar[String].singleOpt
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

## JDBC mappings

As already seen in this documentation, Anorm provides builtins converters between JDBC and JVM types.

### Column parsers

Following table describes which JDBC numeric types (getters on `java.sql.ResultSet`, first column) can be parsed to which Java/Scala types (e.g. integer column can be read as double value).

↓JDBC / JVM➞           | BigDecimal<sup>1</sup> | BigInteger<sup>2</sup> | Boolean | Byte | Double | Float | Int | Long | Short
---------------------- | ---------------------- | ---------------------- | ------- | ---- | ------ | ----- | --- | ---- | -----
BigDecimal<sup>1</sup> | Yes                    | Yes                    | No      | No   | Yes    | No    | Yes | Yes  | No
BigInteger<sup>2</sup> | Yes                    | Yes                    | No      | No   | Yes    | Yes   | Yes | Yes  | No
Boolean                | No                     | No                     | Yes     | Yes  | No     | No    | Yes | Yes  | Yes
Byte                   | Yes                    | No                     | No      | Yes  | Yes    | Yes   | No  | No   | Yes
Double                 | Yes                    | No                     | No      | No   | Yes    | No    | No  | No   | No
Float                  | Yes                    | No                     | No      | No   | Yes    | Yes   | No  | No   | No
Int                    | Yes                    | Yes                    | No      | No   | Yes    | Yes   | Yes | Yes  | No
Long                   | Yes                    | Yes                    | No      | No   | No     | No    | Yes | Yes  | No
Short                  | Yes                    | No                     | No      | Yes  | Yes    | Yes   | No  | No   | Yes

- 1. Types `java.math.BigDecimal` and `scala.math.BigDecimal`.
- 2. Types `java.math.BigInteger` and `scala.math.BigInt`.

Second table shows mapping for other supported types (texts, dates, ...).

↓JDBC / JVM➞         | Array[T]<sup>3</sup> | Char | Date | List<sup>3</sup> | String | UUID<sup>4</sup>
-------------------- | -------------------- | ---- | ---- | ---------------- | ------ | ----------------
Array<sup>5</sup>    | Yes                  | No   | No   | Yes              | No     | No
Clob                 | No                   | Yes  | No   | No               | Yes    | No
Date                 | No                   | No   | Yes  | No               | No     | No
Iterable<sup>6</sup> | Yes                  | No   | Yes  | Yes              | No     | No
Long                 | No                   | No   | Yes  | No               | No     | No
String               | No                   | Yes  | No   | No               | Yes    | Yes
UUID                 | No                   | No   | No   | No               | No     | Yes

- 3. Array which type `T` of elements is supported.
- 4. Type `java.util.UUID`.
- 5. Type `java.sql.Array`.
- 6. Type `java.lang.Iterable[_]`.

Optional column can be parsed as `Option[T]`, as soon as `T` is supported.

Binary data types are also supported.

↓JDBC / JVM➞            | Array[Byte] | InputStream<sup>1</sup>
----------------------- | ----------- | -----------------------
Array[Byte]             | Yes         | Yes
Blob<sup>2</sup>        | Yes         | Yes
Clob<sup>3</sup>        | No          | No
InputStream<sup>4</sup> | Yes         | Yes
Reader<sup>5</sup>      | No          | No

- 1. Type `java.io.InputStream`.
- 2. Type `java.sql.Blob`.
- 3. Type `java.sql.Clob`.
- 4. Type `java.io.Reader`.

CLOBs/TEXTs can be extracted as so:

```scala
SQL("Select name,summary from Country")().map {
  case Row(name: String, summary: java.sql.Clob) => name -> summary
}
```

Here we specifically chose to use `map`, as we want an exception if the row isn't in the format we expect.

Extracting binary data is similarly possible:

```scala
SQL("Select name,image from Country")().map {
  case Row(name: String, image: Array[Byte]) => name -> image
}
```

For types where column support is provided by Anorm, convenient functions are available to ease writing custom parsers. Each of these functions parses column either by name or index (> 1).

```scala
import anorm.SqlParser.str // String function

str("column")
str(1/* columnIndex)
```

Type                    | Function
------------------------|--------------
Array[Byte]             | byteArray
Boolean                 | bool
Byte                    | byte
Date                    | date
Double                  | double
Float                   | float
InputStream<sup>1</sup> | binaryStream
Int                     | int
Long                    | long
Short                   | short
String                  | str

- 1. Type `java.io.InputStream`.

Temporal types from [Joda](http://www.joda.org) are also supported:

↓JDBC / JVM➞ | DateTime<sup>1</sup> | Instant<sup>2</sup>
------------ | -------------------- | -------------------
Date         | Yes                  | Yes
Long         | Yes                  | Yes
Timestamp    | Yes                  | Yes

- 1. Type `org.joda.time.DateTime`.
- 2. Type `org.joda.time.Instant`.

It's possible to add custom mapping, for example if underlying DB doesn't support boolean datatype and returns integer instead. To do so, you have to provide a new implicit conversion for `Column[T]`, where `T` is the target Scala type:

```scala
import anorm.Column

// Custom conversion from JDBC column to Boolean
implicit def columnToBoolean: Column[Boolean] = 
  Column.nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool) // Provided-default case
      case bit: Int      => Right(bit == 1) // Custom conversion
      case _             => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Boolean for column $qualified"))
    }
  }
```

### Parameters

The following table indicates how JVM types are mapped to JDBC parameter types:

JVM                       | JDBC                                                  | Nullable
--------------------------|-------------------------------------------------------|----------
Array[T]<sup>1</sup>      | Array<sup>2</sup> with `T` mapping for each element   | Yes
BigDecimal<sup>3</sup>    | BigDecimal                                            | Yes
BigInteger<sup>4</sup>    | BigDecimal                                            | Yes
Boolean<sup>5</sup>       | Boolean                                               | Yes
Byte<sup>6</sup>          | Byte                                                  | Yes
Char<sup>7</sup>/String   | String                                                | Yes
Date/Timestamp            | Timestamp                                             | Yes
Double<sup>8</sup>        | Double                                                | Yes
Float<sup>9</sup>         | Float                                                 | Yes
Int<sup>10</sup>          | Int                                                   | Yes
List[T]                   | Multi-value<sup>11</sup>, with `T` mapping for each element | No
Long<sup>12</sup>         | Long                                                  | Yes
Object<sup>13</sup>       | Object                                                | Yes
Option[T]                 | `T` being type if some defined value                  | No
Seq[T]                    | Multi-value, with `T` mapping for each element        | No
Set[T]<sup>14</sup>       | Multi-value, with `T` mapping for each element        | No
Short<sup>15</sup>        | Short                                                 | Yes
SortedSet[T]<sup>16</sup> | Multi-value, with `T` mapping for each element        | No
Stream[T]                 | Multi-value, with `T` mapping for each element        | No
UUID                      | String<sup>17</sup>                                   | No
Vector                    | Multi-value, with `T` mapping for each element        | No

- 1. Type Scala `Array[T]`.
- 2. Type `java.sql.Array`.
- 3. Types `java.math.BigDecimal` and `scala.math.BigDecimal`.
- 4. Types `java.math.BigInteger` and `scala.math.BigInt`.
- 5. Types `Boolean` and `java.lang.Boolean`.
- 6. Types `Byte` and `java.lang.Byte`.
- 7. Types `Char` and `java.lang.Character`.
- 8. Types `Double` and `java.lang.Double`.
- 9. Types `Float` and `java.lang.Float`.
- 10. Types `Int` and `java.lang.Integer`.
- 11. Types `Long` and `java.lang.Long`.
- 12. Type `anorm.Object`, wrapping opaque object.
- 13. Multi-value parameter, with one JDBC placeholder (`?`) added for each element.
- 14. Type `scala.collection.immutable.Set`.
- 15. Types `Short` and `java.lang.Short`.
- 16. Type `scala.collection.immutable.SortedSet`.
- 17. Not-null value extracted using `.toString`.

> Passing `None` for a nullable parameter is deprecated, and typesafe `Option.empty[T]` must be use instead.

[Joda](http://www.joda.org) temporal types are supported as parameters:

JVM                  | JDBC
---------------------|-----------
DateTime<sup>1</sup> | Timestamp
Instant<sup>2</sup>  | Timstamp

- 1. Type `org.joda.time.DateTime`.
- 2. Type `org.joda.time.Instant`.

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

If involved type accept `null` value, it must be appropriately handled in conversion. The `NotNullGuard` trait can be used to explicitly refuse `null` values in parameter conversion: `new ToStatement[T] with NotNullGuard { /* ... */ }`.

DB specific parameter can be explicitly passed as opaque value.
In this case at your own risk, `setObject` will be used on statement.

```scala
val anyVal: Any = myVal
SQL("UPDATE t SET v = {opaque}").on('opaque -> anorm.Object(anyVal))
```