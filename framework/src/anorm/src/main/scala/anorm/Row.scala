package anorm

trait Row {
  private[anorm] def metaData: MetaData

  /** Raw data */
  private[anorm] val data: List[Any]

  /**
   * Returns row as list of column values.
   *
   * {{{
   * // Row first column is string "str", second one is integer 2
   * val l: List[Any] = row.asList
   * // l == List[Any]("str", 2)
   * }}}
   */
  lazy val asList: List[Any] = data.foldLeft[List[Any]](Nil) { (l, v) =>
    if (metaData.ms(l.size).nullable) l :+ Option(v) else l :+ v
  }

  /**
   * Returns row as dictionary of value per column name
   *
   * {{{
   * // Row column named 'A' is string "str", column named 'B' is integer 2
   * val m: Map[String, Any] = row.asMap
   * // l == Map[String, Any]("table.A" -> "str", "table.B" -> 2)
   * }}}
   */
  lazy val asMap: Map[String, Any] =
    data.foldLeft[Map[String, Any]](Map.empty) { (m, v) =>
      val d = metaData.ms(m.size)
      val k = d.column.qualified
      if (d.nullable) m + (k -> Option(v)) else m + (k -> v)
    }

  /**
   * Returns parsed column.
   *
   * @param name Column name
   * @param c Column mapping
   *
   * {{{
   * import anorm.Column.columnToString // mapping column to string
   *
   * val res: (String, String) = SQL("SELECT * FROM Test").map(row =>
   *   row("code") -> row("label") // string columns 'code' and 'label'
   * )
   * }}}
   */
  def apply[B](name: String)(implicit c: Column[B]): B =
    unsafeGet(SqlParser.get(name)(c))

  /**
   * Returns parsed column.
   *
   * @param position Column position from 1 to n
   * @param c Column mapping
   *
   * {{{
   * import anorm.Column.columnToString // mapping column to string
   *
   * val res: (String, String) = SQL("SELECT * FROM Test").map(row =>
   *   row(1) -> row(2) // string columns #1 and #2
   * )
   * }}}
   */
  def apply[B](position: Int)(implicit c: Column[B]): B =
    unsafeGet(SqlParser.get(position)(c))

  @inline def unsafeGet[T](rowparser: => RowParser[T]): T =
    MayErr(rowparser(this) match {
      case Success(v) => Right(v)
      case Error(err) => Left(err)
    }).get // TODO: Safe alternative

  // Data per column name
  private lazy val columnsDictionary: Map[String, Any] = {
    @annotation.tailrec
    def loop(meta: List[MetaDataItem], dt: List[Any], r: Map[String, Any]): Map[String, Any] = (meta, dt) match {
      case (m :: ms, d :: ds) => loop(ms, ds,
        r + (m.column.qualified.toUpperCase -> d))
      case _ => r
    }

    loop(metaData.ms, data, Map.empty)
  }

  // Data per column alias
  private lazy val aliasesDictionary: Map[String, Any] = {
    @annotation.tailrec
    def loop(meta: List[MetaDataItem], dt: List[Any], r: Map[String, Any]): Map[String, Any] = (meta, dt) match {
      case (m :: ms, d :: ds) => loop(ms, ds,
        m.column.alias.fold(r) { c => r + (c.toUpperCase -> d) })
      case _ => r
    }

    loop(metaData.ms, data, Map.empty)
  }

  /**
   * Try to get data matching name.
   * @param name Column qualified name, or label/alias
   */
  private[anorm] def get(a: String): MayErr[SqlRequestError, (Any, MetaDataItem)] = for {
    m <- MayErr(metaData.get(a).
      toRight(ColumnNotFound(a, metaData.availableColumns)))
    data <- MayErr(columnsDictionary.get(m.column.qualified.toUpperCase()).
      toRight(ColumnNotFound(m.column.qualified, metaData.availableColumns)))

  } yield (data, m)

  @deprecated(message = "Use [[get]] with alias", since = "2.3.3")
  private[anorm] def getAliased(a: String): MayErr[SqlRequestError, (Any, MetaDataItem)] = for {
    m <- MayErr(metaData.getAliased(a).
      toRight(ColumnNotFound(a, metaData.availableColumns)))
    data <- MayErr(m.column.alias.flatMap(
      l => aliasesDictionary.get(l.toUpperCase())).
      toRight(ColumnNotFound(m.column.alias.getOrElse(a),
        metaData.availableColumns)))

  } yield (data, m)

  /** Try to get data matching index. */
  private[anorm] def getIndexed(i: Int): MayErr[SqlRequestError, (Any, MetaDataItem)] =
    for {
      m <- MayErr(metaData.ms.lift(i).
        toRight(ColumnNotFound(s"#${i + 1}", metaData.availableColumns)))
      d <- MayErr(data.lift(i).
        toRight(ColumnNotFound(m.column.qualified, metaData.availableColumns)))
    } yield (d, m)

}

/** Companion object for row. */
object Row {

  /**
   * Row extractor.
   *
   * {{{
   * import java.util.Locale
   *
   * val l: Option[Locale] =
   *   SQL("Select name,population from Country")().collect {
   *     case Row("France", _) => Some(Locale.FRANCE)
   *     case _ => None
   *   }
   * }}}
   */
  def unapplySeq(row: Row): Option[List[Any]] = Some(row.asList)
}
