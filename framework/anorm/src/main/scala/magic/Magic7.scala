
package anorm {

  import utils.Scala.MayErr
  import utils.Scala.MayErr._

  trait MParser7[A1, A2, A3, A4, A5, A6, A7, R] extends ParserWithId[R] {

    val p1: ColumnTo[A1]

    val p2: ColumnTo[A2]

    val p3: ColumnTo[A3]

    val p4: ColumnTo[A4]

    val p5: ColumnTo[A5]

    val p6: ColumnTo[A6]

    val p7: ColumnTo[A7]

    def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7): R

    val containerName: String
    val columnNames: (String, String, String, String, String, String, String)

    lazy val (name1, name2, name3, name4, name5, name6, name7) = columnNames

    import SqlParser._
    override def apply(input: Input): SqlParser.ParseResult[R] =
      (get[A1](name1)(p1) ~<
        get[A2](name2)(p2) ~<
        get[A3](name3)(p3) ~<
        get[A4](name4)(p4) ~<
        get[A5](name5)(p5) ~<
        get[A6](name6)(p6) ~<
        get[A7](name7)(p7) ^^ { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 => apply(a1, a2, a3, a4, a5, a6, a7) })(input)

    val uniqueId: (Row => MayErr[SqlRequestError, Any]) = null
  }

  trait M7[A1, A2, A3, A4, A5, A6, A7, R] {
    self: MParser7[A1, A2, A3, A4, A5, A6, A7, R] =>

    val pt1: (ColumnTo[A1], ToStatement[A1])
    val pt2: (ColumnTo[A2], ToStatement[A2])
    val pt3: (ColumnTo[A3], ToStatement[A3])
    val pt4: (ColumnTo[A4], ToStatement[A4])
    val pt5: (ColumnTo[A5], ToStatement[A5])
    val pt6: (ColumnTo[A6], ToStatement[A6])
    val pt7: (ColumnTo[A7], ToStatement[A7])

    def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7)]
    def unqualify(columnName: String) = columnName.split('.').last

    def update(v: R)(implicit connection: java.sql.Connection, hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_]) |:| (A3 <:< Pk[_]) |:| (A4 <:< Pk[_]) |:| (A5 <:< Pk[_]) |:| (A6 <:< Pk[_]) |:| (A7 <:< Pk[_])) = {

      val all = ((v, hasId) match {
        case (self(a1, a2, a3, a4, a5, a6, a7), (e1 |:| e2 |:| e3 |:| e4 |:| e5 |:| e6 |:| e7)) =>
          List(
            (e1, unqualify(name1), toParameterValue(a1)(pt1._2)),
            (e2, unqualify(name2), toParameterValue(a2)(pt2._2)),
            (e3, unqualify(name3), toParameterValue(a3)(pt3._2)),
            (e4, unqualify(name4), toParameterValue(a4)(pt4._2)),
            (e5, unqualify(name5), toParameterValue(a5)(pt5._2)),
            (e6, unqualify(name6), toParameterValue(a6)(pt6._2)),
            (e7, unqualify(name7), toParameterValue(a7)(pt7._2)))
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

    def create(v: R)(implicit connection: java.sql.Connection, hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_]) |:| (A3 <:< Pk[_]) |:| (A4 <:< Pk[_]) |:| (A5 <:< Pk[_]) |:| (A6 <:< Pk[_]) |:| (A7 <:< Pk[_])): R = {

      val all = ((v, hasId) match {
        case (self(a1, a2, a3, a4, a5, a6, a7), (e1 |:| e2 |:| e3 |:| e4 |:| e5 |:| e6 |:| e7)) =>
          List(
            (e1, unqualify(name1), toParameterValue(a1)(pt1._2)),
            (e2, unqualify(name2), toParameterValue(a2)(pt2._2)),
            (e3, unqualify(name3), toParameterValue(a3)(pt3._2)),
            (e4, unqualify(name4), toParameterValue(a4)(pt4._2)),
            (e5, unqualify(name5), toParameterValue(a5)(pt5._2)),
            (e6, unqualify(name6), toParameterValue(a6)(pt6._2)),
            (e7, unqualify(name7), toParameterValue(a7)(pt7._2)))
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

      val List(a1, a2, a3, a4, a5, a6, a7) = all.map(_._3.aValue).map({ case NotAssigned => Id(id); case other => other })
      apply(a1.asInstanceOf[A1], a2.asInstanceOf[A2], a3.asInstanceOf[A3], a4.asInstanceOf[A4], a5.asInstanceOf[A5], a6.asInstanceOf[A6], a7.asInstanceOf[A7])

    }
  }

}

