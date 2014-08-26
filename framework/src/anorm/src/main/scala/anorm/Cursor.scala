package anorm

import java.sql.ResultSet

/** Result cursor */
sealed trait Cursor {
  /** Current row */
  def row: Row

  /** Cursor to next row */
  def next: Option[Cursor]
}

/** Cursor companion */
object Cursor {
  import scala.language.reflectiveCalls
  import java.sql.ResultSetMetaData

  /**
   * Returns cursor for next row in given result set.
   *
   * @param rs Result set, must be before first row
   * @return None if there is no result in the set
   */
  private[anorm] def apply(rs: ResultSet): Option[Cursor] =
    if (!rs.next) None else Some(new Cursor {
      val meta = metaData(rs)
      val columns: List[Int] = List.range(1, meta.columnCount + 1)
      val row = ResultRow(meta, columns.map(rs.getObject(_)))

      def next = apply(rs, meta, columns)
    })

  /** Creates cursor after the first one, as meta data is already known. */
  private def apply(rs: ResultSet, meta: MetaData, columns: List[Int]): Option[Cursor] = if (!rs.next) None else Some(new Cursor {
    val row = ResultRow(meta, columns.map(rs.getObject(_)))
    def next = apply(rs)
  })

  /** Returns metadata for given result set. */
  private def metaData(rs: ResultSet): MetaData = {
    val meta = rs.getMetaData()
    val nbColumns = meta.getColumnCount()
    MetaData(List.range(1, nbColumns + 1).map(i =>
      MetaDataItem(column = ColumnName({

        // HACK FOR POSTGRES - Fix in https://github.com/pgjdbc/pgjdbc/pull/107
        if (meta.getClass.getName.startsWith("org.postgresql.")) {
          meta.asInstanceOf[{ def getBaseTableName(i: Int): String }].getBaseTableName(i)
        } else {
          meta.getTableName(i)
        }

      } + "." + meta.getColumnName(i), alias = Option(meta.getColumnLabel(i))),
        nullable = meta.isNullable(i) == ResultSetMetaData.columnNullable,
        clazz = meta.getColumnClassName(i))))
  }

  /** Result row to be parsed. */
  private case class ResultRow(
      metaData: MetaData, data: List[Any]) extends Row {

    override lazy val toString = "Row(" + metaData.ms.zip(data).map(t => s"'${t._1.column}': ${t._2} as ${t._1.clazz}").mkString(", ") + ")"
  }
}
