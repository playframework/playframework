package anorm {

  trait WithDefaults {

    val defaultConvention: PartialFunction[AnalyserInfo, String]

    import java.lang.reflect.Method

    def getParametersNames(m: Method): Seq[String] = {
      utils.Scala.lookupParameterNames(m)
    }

    abstract class AbstractMagicParser2[A1, A2, R](
        tableDescription: Option[Description2] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], r: Manifest[R]) extends MParser2[A1, A2, R] {

      lazy val p1 = c1
      lazy val p2 = c2

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 2)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2) => (a1, a2) }
          .get

      }
    }

    abstract class AbstractMagic2[A1, A2, R](
      tableDescription: Option[Description2] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), r: Manifest[R]) extends AbstractMagicParser2[A1, A2, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, r) with M2[A1, A2, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
    }

    case class MagicParser2[A1, A2, R](
        cons: Function2[A1, A2, R],
        tableDescription: Option[Description2] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], r: Manifest[R]) extends AbstractMagicParser2[A1, A2, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2): R = cons(a1, a2)

    }
    case class Magic2[A1, A2, R](
      companion: Companion2[A1, A2, R],
      tableDescription: Option[Description2] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), r: Manifest[R]) extends AbstractMagic2[A1, A2, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2): R = companion(a1, a2)
      def unapply(r: R): Option[(A1, A2)] = companion.unapply(r)
    }

    case class Description2(table: String, columns: Option[(String, String)] = None)

    trait Companion2[A1, A2, R] {
      def apply(a1: A1, a2: A2): R
      def unapply(r: R): Option[(A1, A2)]
    }

    abstract class AbstractMagicParser3[A1, A2, A3, R](
        tableDescription: Option[Description3] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], r: Manifest[R]) extends MParser3[A1, A2, A3, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 3)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3) => (a1, a2, a3) }
          .get

      }
    }

    abstract class AbstractMagic3[A1, A2, A3, R](
      tableDescription: Option[Description3] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), r: Manifest[R]) extends AbstractMagicParser3[A1, A2, A3, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, r) with M3[A1, A2, A3, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
    }

    case class MagicParser3[A1, A2, A3, R](
        cons: Function3[A1, A2, A3, R],
        tableDescription: Option[Description3] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], r: Manifest[R]) extends AbstractMagicParser3[A1, A2, A3, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3): R = cons(a1, a2, a3)

    }
    case class Magic3[A1, A2, A3, R](
      companion: Companion3[A1, A2, A3, R],
      tableDescription: Option[Description3] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), r: Manifest[R]) extends AbstractMagic3[A1, A2, A3, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3): R = companion(a1, a2, a3)
      def unapply(r: R): Option[(A1, A2, A3)] = companion.unapply(r)
    }

    case class Description3(table: String, columns: Option[(String, String, String)] = None)

    trait Companion3[A1, A2, A3, R] {
      def apply(a1: A1, a2: A2, a3: A3): R
      def unapply(r: R): Option[(A1, A2, A3)]
    }

    abstract class AbstractMagicParser4[A1, A2, A3, A4, R](
        tableDescription: Option[Description4] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], r: Manifest[R]) extends MParser4[A1, A2, A3, A4, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 4)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4) => (a1, a2, a3, a4) }
          .get

      }
    }

    abstract class AbstractMagic4[A1, A2, A3, A4, R](
      tableDescription: Option[Description4] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), r: Manifest[R]) extends AbstractMagicParser4[A1, A2, A3, A4, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, r) with M4[A1, A2, A3, A4, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
    }

    case class MagicParser4[A1, A2, A3, A4, R](
        cons: Function4[A1, A2, A3, A4, R],
        tableDescription: Option[Description4] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], r: Manifest[R]) extends AbstractMagicParser4[A1, A2, A3, A4, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4): R = cons(a1, a2, a3, a4)

    }
    case class Magic4[A1, A2, A3, A4, R](
      companion: Companion4[A1, A2, A3, A4, R],
      tableDescription: Option[Description4] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), r: Manifest[R]) extends AbstractMagic4[A1, A2, A3, A4, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4): R = companion(a1, a2, a3, a4)
      def unapply(r: R): Option[(A1, A2, A3, A4)] = companion.unapply(r)
    }

    case class Description4(table: String, columns: Option[(String, String, String, String)] = None)

    trait Companion4[A1, A2, A3, A4, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4): R
      def unapply(r: R): Option[(A1, A2, A3, A4)]
    }

    abstract class AbstractMagicParser5[A1, A2, A3, A4, A5, R](
        tableDescription: Option[Description5] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], r: Manifest[R]) extends MParser5[A1, A2, A3, A4, A5, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 5)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5) => (a1, a2, a3, a4, a5) }
          .get

      }
    }

    abstract class AbstractMagic5[A1, A2, A3, A4, A5, R](
      tableDescription: Option[Description5] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), r: Manifest[R]) extends AbstractMagicParser5[A1, A2, A3, A4, A5, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, r) with M5[A1, A2, A3, A4, A5, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
    }

    case class MagicParser5[A1, A2, A3, A4, A5, R](
        cons: Function5[A1, A2, A3, A4, A5, R],
        tableDescription: Option[Description5] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], r: Manifest[R]) extends AbstractMagicParser5[A1, A2, A3, A4, A5, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5): R = cons(a1, a2, a3, a4, a5)

    }
    case class Magic5[A1, A2, A3, A4, A5, R](
      companion: Companion5[A1, A2, A3, A4, A5, R],
      tableDescription: Option[Description5] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), r: Manifest[R]) extends AbstractMagic5[A1, A2, A3, A4, A5, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5): R = companion(a1, a2, a3, a4, a5)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5)] = companion.unapply(r)
    }

    case class Description5(table: String, columns: Option[(String, String, String, String, String)] = None)

    trait Companion5[A1, A2, A3, A4, A5, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5)]
    }

    abstract class AbstractMagicParser6[A1, A2, A3, A4, A5, A6, R](
        tableDescription: Option[Description6] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], r: Manifest[R]) extends MParser6[A1, A2, A3, A4, A5, A6, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 6)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6) => (a1, a2, a3, a4, a5, a6) }
          .get

      }
    }

    abstract class AbstractMagic6[A1, A2, A3, A4, A5, A6, R](
      tableDescription: Option[Description6] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), r: Manifest[R]) extends AbstractMagicParser6[A1, A2, A3, A4, A5, A6, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, r) with M6[A1, A2, A3, A4, A5, A6, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
    }

    case class MagicParser6[A1, A2, A3, A4, A5, A6, R](
        cons: Function6[A1, A2, A3, A4, A5, A6, R],
        tableDescription: Option[Description6] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], r: Manifest[R]) extends AbstractMagicParser6[A1, A2, A3, A4, A5, A6, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6): R = cons(a1, a2, a3, a4, a5, a6)

    }
    case class Magic6[A1, A2, A3, A4, A5, A6, R](
      companion: Companion6[A1, A2, A3, A4, A5, A6, R],
      tableDescription: Option[Description6] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), r: Manifest[R]) extends AbstractMagic6[A1, A2, A3, A4, A5, A6, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6): R = companion(a1, a2, a3, a4, a5, a6)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6)] = companion.unapply(r)
    }

    case class Description6(table: String, columns: Option[(String, String, String, String, String, String)] = None)

    trait Companion6[A1, A2, A3, A4, A5, A6, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6)]
    }

    abstract class AbstractMagicParser7[A1, A2, A3, A4, A5, A6, A7, R](
        tableDescription: Option[Description7] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], r: Manifest[R]) extends MParser7[A1, A2, A3, A4, A5, A6, A7, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 7)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7) => (a1, a2, a3, a4, a5, a6, a7) }
          .get

      }
    }

    abstract class AbstractMagic7[A1, A2, A3, A4, A5, A6, A7, R](
      tableDescription: Option[Description7] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), r: Manifest[R]) extends AbstractMagicParser7[A1, A2, A3, A4, A5, A6, A7, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, r) with M7[A1, A2, A3, A4, A5, A6, A7, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
    }

    case class MagicParser7[A1, A2, A3, A4, A5, A6, A7, R](
        cons: Function7[A1, A2, A3, A4, A5, A6, A7, R],
        tableDescription: Option[Description7] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], r: Manifest[R]) extends AbstractMagicParser7[A1, A2, A3, A4, A5, A6, A7, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7): R = cons(a1, a2, a3, a4, a5, a6, a7)

    }
    case class Magic7[A1, A2, A3, A4, A5, A6, A7, R](
      companion: Companion7[A1, A2, A3, A4, A5, A6, A7, R],
      tableDescription: Option[Description7] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), r: Manifest[R]) extends AbstractMagic7[A1, A2, A3, A4, A5, A6, A7, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7): R = companion(a1, a2, a3, a4, a5, a6, a7)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7)] = companion.unapply(r)
    }

    case class Description7(table: String, columns: Option[(String, String, String, String, String, String, String)] = None)

    trait Companion7[A1, A2, A3, A4, A5, A6, A7, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7)]
    }

    abstract class AbstractMagicParser8[A1, A2, A3, A4, A5, A6, A7, A8, R](
        tableDescription: Option[Description8] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], r: Manifest[R]) extends MParser8[A1, A2, A3, A4, A5, A6, A7, A8, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 8)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8) => (a1, a2, a3, a4, a5, a6, a7, a8) }
          .get

      }
    }

    abstract class AbstractMagic8[A1, A2, A3, A4, A5, A6, A7, A8, R](
      tableDescription: Option[Description8] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), r: Manifest[R]) extends AbstractMagicParser8[A1, A2, A3, A4, A5, A6, A7, A8, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, r) with M8[A1, A2, A3, A4, A5, A6, A7, A8, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
    }

    case class MagicParser8[A1, A2, A3, A4, A5, A6, A7, A8, R](
        cons: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R],
        tableDescription: Option[Description8] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], r: Manifest[R]) extends AbstractMagicParser8[A1, A2, A3, A4, A5, A6, A7, A8, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8): R = cons(a1, a2, a3, a4, a5, a6, a7, a8)

    }
    case class Magic8[A1, A2, A3, A4, A5, A6, A7, A8, R](
      companion: Companion8[A1, A2, A3, A4, A5, A6, A7, A8, R],
      tableDescription: Option[Description8] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), r: Manifest[R]) extends AbstractMagic8[A1, A2, A3, A4, A5, A6, A7, A8, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8): R = companion(a1, a2, a3, a4, a5, a6, a7, a8)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8)] = companion.unapply(r)
    }

    case class Description8(table: String, columns: Option[(String, String, String, String, String, String, String, String)] = None)

    trait Companion8[A1, A2, A3, A4, A5, A6, A7, A8, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8)]
    }

    abstract class AbstractMagicParser9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](
        tableDescription: Option[Description9] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], r: Manifest[R]) extends MParser9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 9)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9) => (a1, a2, a3, a4, a5, a6, a7, a8, a9) }
          .get

      }
    }

    abstract class AbstractMagic9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](
      tableDescription: Option[Description9] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), r: Manifest[R]) extends AbstractMagicParser9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, r) with M9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
    }

    case class MagicParser9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](
        cons: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R],
        tableDescription: Option[Description9] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], r: Manifest[R]) extends AbstractMagicParser9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9)

    }
    case class Magic9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](
      companion: Companion9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R],
      tableDescription: Option[Description9] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), r: Manifest[R]) extends AbstractMagic9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] = companion.unapply(r)
    }

    case class Description9(table: String, columns: Option[(String, String, String, String, String, String, String, String, String)] = None)

    trait Companion9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]
    }

    abstract class AbstractMagicParser10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](
        tableDescription: Option[Description10] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], r: Manifest[R]) extends MParser10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 10)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) }
          .get

      }
    }

    abstract class AbstractMagic10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](
      tableDescription: Option[Description10] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), r: Manifest[R]) extends AbstractMagicParser10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, r) with M10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
    }

    case class MagicParser10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](
        cons: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R],
        tableDescription: Option[Description10] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], r: Manifest[R]) extends AbstractMagicParser10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)

    }
    case class Magic10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](
      companion: Companion10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R],
      tableDescription: Option[Description10] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), r: Manifest[R]) extends AbstractMagic10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] = companion.unapply(r)
    }

    case class Description10(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]
    }

    abstract class AbstractMagicParser11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](
        tableDescription: Option[Description11] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], r: Manifest[R]) extends MParser11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 11)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) }
          .get

      }
    }

    abstract class AbstractMagic11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](
      tableDescription: Option[Description11] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), r: Manifest[R]) extends AbstractMagicParser11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, r) with M11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
    }

    case class MagicParser11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](
        cons: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R],
        tableDescription: Option[Description11] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], r: Manifest[R]) extends AbstractMagicParser11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)

    }
    case class Magic11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](
      companion: Companion11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R],
      tableDescription: Option[Description11] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), r: Manifest[R]) extends AbstractMagic11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] = companion.unapply(r)
    }

    case class Description11(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]
    }

    abstract class AbstractMagicParser12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](
        tableDescription: Option[Description12] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], r: Manifest[R]) extends MParser12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 12)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) }
          .get

      }
    }

    abstract class AbstractMagic12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](
      tableDescription: Option[Description12] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), r: Manifest[R]) extends AbstractMagicParser12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, r) with M12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
    }

    case class MagicParser12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](
        cons: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R],
        tableDescription: Option[Description12] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], r: Manifest[R]) extends AbstractMagicParser12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)

    }
    case class Magic12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](
      companion: Companion12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R],
      tableDescription: Option[Description12] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), r: Manifest[R]) extends AbstractMagic12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] = companion.unapply(r)
    }

    case class Description12(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]
    }

    abstract class AbstractMagicParser13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](
        tableDescription: Option[Description13] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], r: Manifest[R]) extends MParser13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 13)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) }
          .get

      }
    }

    abstract class AbstractMagic13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](
      tableDescription: Option[Description13] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), r: Manifest[R]) extends AbstractMagicParser13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, r) with M13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
    }

    case class MagicParser13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](
        cons: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R],
        tableDescription: Option[Description13] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], r: Manifest[R]) extends AbstractMagicParser13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)

    }
    case class Magic13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](
      companion: Companion13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R],
      tableDescription: Option[Description13] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), r: Manifest[R]) extends AbstractMagic13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] = companion.unapply(r)
    }

    case class Description13(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]
    }

    abstract class AbstractMagicParser14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](
        tableDescription: Option[Description14] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], r: Manifest[R]) extends MParser14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 14)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) }
          .get

      }
    }

    abstract class AbstractMagic14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](
      tableDescription: Option[Description14] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), r: Manifest[R]) extends AbstractMagicParser14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, r) with M14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
    }

    case class MagicParser14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](
        cons: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R],
        tableDescription: Option[Description14] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], r: Manifest[R]) extends AbstractMagicParser14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)

    }
    case class Magic14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](
      companion: Companion14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R],
      tableDescription: Option[Description14] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), r: Manifest[R]) extends AbstractMagic14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)] = companion.unapply(r)
    }

    case class Description14(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]
    }

    abstract class AbstractMagicParser15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](
        tableDescription: Option[Description15] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], r: Manifest[R]) extends MParser15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 15)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) }
          .get

      }
    }

    abstract class AbstractMagic15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](
      tableDescription: Option[Description15] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), r: Manifest[R]) extends AbstractMagicParser15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, r) with M15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
    }

    case class MagicParser15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](
        cons: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R],
        tableDescription: Option[Description15] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], r: Manifest[R]) extends AbstractMagicParser15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)

    }
    case class Magic15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](
      companion: Companion15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R],
      tableDescription: Option[Description15] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), r: Manifest[R]) extends AbstractMagic15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)] = companion.unapply(r)
    }

    case class Description15(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]
    }

    abstract class AbstractMagicParser16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](
        tableDescription: Option[Description16] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], r: Manifest[R]) extends MParser16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 16)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) }
          .get

      }
    }

    abstract class AbstractMagic16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](
      tableDescription: Option[Description16] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), r: Manifest[R]) extends AbstractMagicParser16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, r) with M16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
    }

    case class MagicParser16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](
        cons: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R],
        tableDescription: Option[Description16] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], r: Manifest[R]) extends AbstractMagicParser16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)

    }
    case class Magic16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](
      companion: Companion16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R],
      tableDescription: Option[Description16] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), r: Manifest[R]) extends AbstractMagic16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)] = companion.unapply(r)
    }

    case class Description16(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]
    }

    abstract class AbstractMagicParser17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](
        tableDescription: Option[Description17] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], r: Manifest[R]) extends MParser17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 17)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) }
          .get

      }
    }

    abstract class AbstractMagic17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](
      tableDescription: Option[Description17] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), r: Manifest[R]) extends AbstractMagicParser17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, r) with M17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
    }

    case class MagicParser17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](
        cons: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R],
        tableDescription: Option[Description17] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], r: Manifest[R]) extends AbstractMagicParser17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)

    }
    case class Magic17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](
      companion: Companion17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R],
      tableDescription: Option[Description17] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), r: Manifest[R]) extends AbstractMagic17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)] = companion.unapply(r)
    }

    case class Description17(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]
    }

    abstract class AbstractMagicParser18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](
        tableDescription: Option[Description18] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], r: Manifest[R]) extends MParser18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17
      lazy val p18 = c18

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 18)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) }
          .get

      }
    }

    abstract class AbstractMagic18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](
      tableDescription: Option[Description18] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), r: Manifest[R]) extends AbstractMagicParser18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, ptt18._1, r) with M18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
      lazy val pt18 = ptt18
    }

    case class MagicParser18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](
        cons: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R],
        tableDescription: Option[Description18] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], r: Manifest[R]) extends AbstractMagicParser18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)

    }
    case class Magic18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](
      companion: Companion18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R],
      tableDescription: Option[Description18] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), r: Manifest[R]) extends AbstractMagic18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, ptt18, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)] = companion.unapply(r)
    }

    case class Description18(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]
    }

    abstract class AbstractMagicParser19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](
        tableDescription: Option[Description19] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], r: Manifest[R]) extends MParser19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17
      lazy val p18 = c18
      lazy val p19 = c19

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 19)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) }
          .get

      }
    }

    abstract class AbstractMagic19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](
      tableDescription: Option[Description19] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), r: Manifest[R]) extends AbstractMagicParser19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, ptt18._1, ptt19._1, r) with M19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
      lazy val pt18 = ptt18
      lazy val pt19 = ptt19
    }

    case class MagicParser19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](
        cons: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R],
        tableDescription: Option[Description19] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], r: Manifest[R]) extends AbstractMagicParser19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)

    }
    case class Magic19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](
      companion: Companion19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R],
      tableDescription: Option[Description19] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), r: Manifest[R]) extends AbstractMagic19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, ptt18, ptt19, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)] = companion.unapply(r)
    }

    case class Description19(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]
    }

    abstract class AbstractMagicParser20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](
        tableDescription: Option[Description20] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], r: Manifest[R]) extends MParser20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17
      lazy val p18 = c18
      lazy val p19 = c19
      lazy val p20 = c20

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 20)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) }
          .get

      }
    }

    abstract class AbstractMagic20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](
      tableDescription: Option[Description20] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), r: Manifest[R]) extends AbstractMagicParser20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, ptt18._1, ptt19._1, ptt20._1, r) with M20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
      lazy val pt18 = ptt18
      lazy val pt19 = ptt19
      lazy val pt20 = ptt20
    }

    case class MagicParser20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](
        cons: Function20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R],
        tableDescription: Option[Description20] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], r: Manifest[R]) extends AbstractMagicParser20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)

    }
    case class Magic20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](
      companion: Companion20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R],
      tableDescription: Option[Description20] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), r: Manifest[R]) extends AbstractMagic20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, ptt18, ptt19, ptt20, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] = companion.unapply(r)
    }

    case class Description20(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)]
    }

    abstract class AbstractMagicParser21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](
        tableDescription: Option[Description21] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], c21: ColumnTo[A21], r: Manifest[R]) extends MParser21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17
      lazy val p18 = c18
      lazy val p19 = c19
      lazy val p20 = c20
      lazy val p21 = c21

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 21)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) }
          .get

      }
    }

    abstract class AbstractMagic21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](
      tableDescription: Option[Description21] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), ptt21: (ColumnTo[A21], ToStatement[A21]), r: Manifest[R]) extends AbstractMagicParser21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, ptt18._1, ptt19._1, ptt20._1, ptt21._1, r) with M21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
      lazy val pt18 = ptt18
      lazy val pt19 = ptt19
      lazy val pt20 = ptt20
      lazy val pt21 = ptt21
    }

    case class MagicParser21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](
        cons: Function21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R],
        tableDescription: Option[Description21] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], c21: ColumnTo[A21], r: Manifest[R]) extends AbstractMagicParser21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21)

    }
    case class Magic21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](
      companion: Companion21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R],
      tableDescription: Option[Description21] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), ptt21: (ColumnTo[A21], ToStatement[A21]), r: Manifest[R]) extends AbstractMagic21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, ptt18, ptt19, ptt20, ptt21, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] = companion.unapply(r)
    }

    case class Description21(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)]
    }

    abstract class AbstractMagicParser22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](
        tableDescription: Option[Description22] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], c21: ColumnTo[A21], c22: ColumnTo[A22], r: Manifest[R]) extends MParser22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R] {

      lazy val p1 = c1
      lazy val p2 = c2
      lazy val p3 = c3
      lazy val p4 = c4
      lazy val p5 = c5
      lazy val p6 = c6
      lazy val p7 = c7
      lazy val p8 = c8
      lazy val p9 = c9
      lazy val p10 = c10
      lazy val p11 = c11
      lazy val p12 = c12
      lazy val p13 = c13
      lazy val p14 = c14
      lazy val p15 = c15
      lazy val p16 = c16
      lazy val p17 = c17
      lazy val p18 = c18
      lazy val p19 = c19
      lazy val p20 = c20
      lazy val p21 = c21
      lazy val p22 = c22

      lazy val typeName = r.erasure.getSimpleName
      lazy val containerName = tableDescription.map(_.table).orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

      def thisClass: Class[_] = implicitly[Manifest[this.type]].erasure

      lazy val columnNames = tableDescription.flatMap(_.columns).getOrElse {
        thisClass.getDeclaredMethods()
          .filter(_.getName() == "apply")
          .find(_.getParameterTypes().length == 22)
          .map(getParametersNames)
          .map(_.map(c => conventions(ColumnC(containerName, c))))
          .collect { case Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) }
          .get

      }
    }

    abstract class AbstractMagic22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](
      tableDescription: Option[Description22] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), ptt21: (ColumnTo[A21], ToStatement[A21]), ptt22: (ColumnTo[A22], ToStatement[A22]), r: Manifest[R]) extends AbstractMagicParser22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](tableDescription = tableDescription, conventions = conventions)(
      ptt1._1, ptt2._1, ptt3._1, ptt4._1, ptt5._1, ptt6._1, ptt7._1, ptt8._1, ptt9._1, ptt10._1, ptt11._1, ptt12._1, ptt13._1, ptt14._1, ptt15._1, ptt16._1, ptt17._1, ptt18._1, ptt19._1, ptt20._1, ptt21._1, ptt22._1, r) with M22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R] {

      lazy val pt1 = ptt1
      lazy val pt2 = ptt2
      lazy val pt3 = ptt3
      lazy val pt4 = ptt4
      lazy val pt5 = ptt5
      lazy val pt6 = ptt6
      lazy val pt7 = ptt7
      lazy val pt8 = ptt8
      lazy val pt9 = ptt9
      lazy val pt10 = ptt10
      lazy val pt11 = ptt11
      lazy val pt12 = ptt12
      lazy val pt13 = ptt13
      lazy val pt14 = ptt14
      lazy val pt15 = ptt15
      lazy val pt16 = ptt16
      lazy val pt17 = ptt17
      lazy val pt18 = ptt18
      lazy val pt19 = ptt19
      lazy val pt20 = ptt20
      lazy val pt21 = ptt21
      lazy val pt22 = ptt22
    }

    case class MagicParser22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](
        cons: Function22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R],
        tableDescription: Option[Description22] = None,
        conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit c1: ColumnTo[A1], c2: ColumnTo[A2], c3: ColumnTo[A3], c4: ColumnTo[A4], c5: ColumnTo[A5], c6: ColumnTo[A6], c7: ColumnTo[A7], c8: ColumnTo[A8], c9: ColumnTo[A9], c10: ColumnTo[A10], c11: ColumnTo[A11], c12: ColumnTo[A12], c13: ColumnTo[A13], c14: ColumnTo[A14], c15: ColumnTo[A15], c16: ColumnTo[A16], c17: ColumnTo[A17], c18: ColumnTo[A18], c19: ColumnTo[A19], c20: ColumnTo[A20], c21: ColumnTo[A21], c22: ColumnTo[A22], r: Manifest[R]) extends AbstractMagicParser22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](tableDescription = tableDescription, conventions = conventions) {
      override def thisClass = cons.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21, a22: A22): R = cons(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22)

    }
    case class Magic22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](
      companion: Companion22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R],
      tableDescription: Option[Description22] = None,
      conventions: PartialFunction[AnalyserInfo, String] = defaultConvention)(implicit ptt1: (ColumnTo[A1], ToStatement[A1]), ptt2: (ColumnTo[A2], ToStatement[A2]), ptt3: (ColumnTo[A3], ToStatement[A3]), ptt4: (ColumnTo[A4], ToStatement[A4]), ptt5: (ColumnTo[A5], ToStatement[A5]), ptt6: (ColumnTo[A6], ToStatement[A6]), ptt7: (ColumnTo[A7], ToStatement[A7]), ptt8: (ColumnTo[A8], ToStatement[A8]), ptt9: (ColumnTo[A9], ToStatement[A9]), ptt10: (ColumnTo[A10], ToStatement[A10]), ptt11: (ColumnTo[A11], ToStatement[A11]), ptt12: (ColumnTo[A12], ToStatement[A12]), ptt13: (ColumnTo[A13], ToStatement[A13]), ptt14: (ColumnTo[A14], ToStatement[A14]), ptt15: (ColumnTo[A15], ToStatement[A15]), ptt16: (ColumnTo[A16], ToStatement[A16]), ptt17: (ColumnTo[A17], ToStatement[A17]), ptt18: (ColumnTo[A18], ToStatement[A18]), ptt19: (ColumnTo[A19], ToStatement[A19]), ptt20: (ColumnTo[A20], ToStatement[A20]), ptt21: (ColumnTo[A21], ToStatement[A21]), ptt22: (ColumnTo[A22], ToStatement[A22]), r: Manifest[R]) extends AbstractMagic22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R](
      tableDescription = tableDescription,
      conventions = conventions)(ptt1, ptt2, ptt3, ptt4, ptt5, ptt6, ptt7, ptt8, ptt9, ptt10, ptt11, ptt12, ptt13, ptt14, ptt15, ptt16, ptt17, ptt18, ptt19, ptt20, ptt21, ptt22, r) {
      override def thisClass = companion.getClass
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21, a22: A22): R = companion(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22)
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)] = companion.unapply(r)
    }

    case class Description22(table: String, columns: Option[(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)] = None)

    trait Companion22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R] {
      def apply(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21, a22: A22): R
      def unapply(r: R): Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)]
    }

  }
}

