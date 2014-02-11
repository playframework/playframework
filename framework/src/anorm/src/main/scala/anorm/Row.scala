package anorm

trait Row {
  val metaData: MetaData

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

  private def get[A](a: String)(implicit c: Column[A]): MayErr[SqlRequestError, A] = SqlParser.get(a)(c)(this) match {
    case Success(a) => Right(a)
    case Error(e) => Left(e)
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
  def apply[B](name: String)(implicit c: Column[B]): B = get[B](name)(c).get

  // TODO: Optimize
  private lazy val columnsDictionary: Map[String, Any] =
    metaData.ms.map(_.column.qualified.toUpperCase()).zip(data).toMap

  private lazy val aliasesDictionary: Map[String, Any] =
    metaData.ms.flatMap(_.column.alias.map(_.toUpperCase())).zip(data).toMap

  /* Type alias for tuple extracted from metadata item */
  private type MetaTuple = (ColumnName, Boolean, String)

  private[anorm] def get1(a: String): MayErr[SqlRequestError, (Any, MetaDataItem)] = for (
    meta <- implicitly[MayErr[SqlRequestError, MetaTuple]](
      metaData.get(a).toRight(ColumnNotFound(a, metaData.availableColumns)));
    m = MetaDataItem.tupled(meta);
    data <- implicitly[MayErr[SqlRequestError, Any]](
      columnsDictionary.get(m.column.qualified.toUpperCase()).
        toRight(ColumnNotFound(
          m.column.qualified, metaData.availableColumns)))

  ) yield (data, m)

  private[anorm] def getAliased(a: String): MayErr[SqlRequestError, (Any, MetaDataItem)] = for (
    meta <- implicitly[MayErr[SqlRequestError, MetaTuple]](
      metaData.getAliased(a).toRight(
        ColumnNotFound(a, metaData.availableColumns)));
    m = MetaDataItem.tupled(meta);
    data <- implicitly[MayErr[SqlRequestError, Any]](
      m.column.alias.flatMap(a => aliasesDictionary.get(a.toUpperCase())).
        toRight(ColumnNotFound(m.column.alias.getOrElse(a),
          metaData.availableColumns)))

  ) yield (data, m)

  /** Try to get data matching index. */
  private[anorm] def getIndexed(i: Int): MayErr[SqlRequestError, (Any, MetaDataItem)] =
    for {
      m <- implicitly[MayErr[SqlRequestError, MetaDataItem]](
        metaData.ms.lift(i).toRight(
          ColumnNotFound(s"#${i + 1}", metaData.availableColumns)));
      d <- implicitly[MayErr[SqlRequestError, Any]](
        data.lift(i).toRight(
          ColumnNotFound(m.column.qualified, metaData.availableColumns)))
    } yield (d, m)

}
