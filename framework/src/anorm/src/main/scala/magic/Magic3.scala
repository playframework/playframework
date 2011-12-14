
package anorm {

  import utils.Scala.MayErr
  import utils.Scala.MayErr._

  trait MParser3[A1, A2, A3, R] extends ParserWithId[R] {

    val p1: ColumnTo[A1]

    val p2: ColumnTo[A2]

    val p3: ColumnTo[A3]

    def apply(a1: A1, a2: A2, a3: A3): R

    val containerName: String
    val columnNames: (String, String, String)

    lazy val (name1, name2, name3) = columnNames

    import SqlParser._
    override def apply(input: Input): SqlParser.ParseResult[R] =
      (get[A1](name1)(p1) ~<
        get[A2](name2)(p2) ~<
        get[A3](name3)(p3) ^^ { case a1 ~ a2 ~ a3 => apply(a1, a2, a3) })(input)

    val uniqueId: (Row => MayErr[SqlRequestError, Any]) = null
  }

  trait M3[A1, A2, A3, R] {
    self: MParser3[A1, A2, A3, R] =>

    val pt1: (ColumnTo[A1], ToStatement[A1])
    val pt2: (ColumnTo[A2], ToStatement[A2])
    val pt3: (ColumnTo[A3], ToStatement[A3])

    def unapply(r: R): Option[(A1, A2, A3)]
    def unqualify(columnName: String) = columnName.split('.').last

    def update(v: R)(implicit connection: java.sql.Connection, hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_]) |:| (A3 <:< Pk[_])) = {

      val all = ((v, hasId) match {
        case (self(a1, a2, a3), (e1 |:| e2 |:| e3)) =>
          List(
            (e1, unqualify(name1), toParameterValue(a1)(pt1._2)),
            (e2, unqualify(name2), toParameterValue(a2)(pt2._2)),
            (e3, unqualify(name3), toParameterValue(a3)(pt3._2)))
      })

      val (ids, toSet) = all.partition(_._1.isDefined)
      if (ids == all) throw new Exception("everything is a Pk, nothing left to set!")

      val toUpdate = toSet.map(_._2).map(n => n + " = " + "{" + n + "}").mkString(", ")

      import Sql._

      sql("update " + containerName + " set " + toUpdate +
        " where " + ids.map(_._2).map(n => n + " = " + "{" + n + "}").mkString(" and "))
        .on(all.map(v => (v._2, v._3)): _*)
        .executeUpdate()

    }

    def create(v: R)(implicit connection: java.sql.Connection, hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_]) |:| (A3 <:< Pk[_])): R = {

      val all = ((v, hasId) match {
        case (self(a1, a2, a3), (e1 |:| e2 |:| e3)) =>
          List(
            (e1, unqualify(name1), toParameterValue(a1)(pt1._2)),
            (e2, unqualify(name2), toParameterValue(a2)(pt2._2)),
            (e3, unqualify(name3), toParameterValue(a3)(pt3._2)))
      })

      val (notSetIds, toSet) = all.partition(i => i._1.isDefined && i._3.aValue == NotAssigned)
      if (notSetIds.length > 1) throw new Exception("multi ids not supported")
      val toInsert = toSet.map(_._2)

      import Sql._
      import scala.util.control.Exception._
      import SqlParser._

      val idParser: SqlParser.Parser[_] = {
        SqlParser.RowParser(row =>
          row.asList.headOption.flatMap(a =>
            (if (a.isInstanceOf[Option[_]]) a else Option(a)).asInstanceOf[Option[_]]).toRight(NoColumnsInReturnedResult))
      }

      val (statement, ok) = sql("insert into " + containerName + " ( " + toInsert.mkString(", ") + " ) values ( " + toInsert.map("{" + _ + "}").mkString(", ") + ")")
        .on(all.map(v => (v._2, v._3)): _*)
        .execute1(getGeneratedKeys = true)

      val rs = statement.getGeneratedKeys();
      val id = idParser(StreamReader(Sql.resultSetToStream(rs))).get

      val List(a1, a2, a3) = all.map(_._3.aValue).map({ case NotAssigned => Id(id); case other => other })
      apply(a1.asInstanceOf[A1], a2.asInstanceOf[A2], a3.asInstanceOf[A3])

    }
  }

}

