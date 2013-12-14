package anorm

import TupleFlattener._
import SqlParser.{ bool, str, int, long, get }

import acolyte.RowLists._
import acolyte.Rows._
import acolyte.Acolyte.{ connection, handleQuery }
import acolyte.Implicits._

object TupleFlattenerSpec extends org.specs2.mutable.Specification {
  "Tuple flattener" title

  "Raw tuple-like" should {
    "be flatten from 2 columns to Tuple2" in {
      val schema = rowList2(classOf[String] -> "A", classOf[Int] -> "B")

      withQueryResult(schema :+ row2("A", 2)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple2("A", 2)

      }
    }

    "be flatten from 3 columns to Tuple3" in {
      val schema = rowList3(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C")

      withQueryResult(schema :+ row3("A", 2, 3l)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple3("A", 2, 3l)

      }
    }

    "be flatten from 4 columns to Tuple4" in {
      val schema = rowList4(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D")

      withQueryResult(schema :+ row4("A", 2, 3l, 4.56d)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple4("A", 2, 3l, 4.56d)

      }
    }

    "be flatten from 5 columns to Tuple5" in {
      val schema = rowList5(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E")

      withQueryResult(schema :+ row5("A", 2, 3l, 4.56d, 9.toShort)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple5("A", 2, 3l, 4.56d, 9.toShort)

      }
    }

    "be flatten from 6 columns to Tuple6" in {
      val schema = rowList6(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F")

      withQueryResult(schema :+ row6("A", 2, 3l, 4.56d, 9.toShort, 10.toByte)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple6("A", 2, 3l, 4.56d, 9.toShort, 10.toByte)

      }
    }

    "be flatten from 7 columns to Tuple7" in {
      val schema = rowList7(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G")

      withQueryResult(schema :+ row7("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple7("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true)

      }
    }

    "be flatten from 8 columns to Tuple8" in {
      val schema = rowList8(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H")

      withQueryResult(schema :+ row8("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B")) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple8("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B")

      }
    }

    "be flatten from 9 columns to Tuple9" in {
      val schema = rowList9(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I")

      withQueryResult(schema :+ row9("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple9("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3)

      }
    }

    "be flatten from 10 columns to Tuple10" in {
      val schema = rowList10(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J")

      withQueryResult(schema :+ row10("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple10("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l)

      }
    }

    "be flatten from 11 columns to Tuple11" in {
      val schema = rowList11(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K")

      withQueryResult(schema :+ row11("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple11("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d)

      }
    }

    "be flatten from 12 columns to Tuple12" in {
      val schema = rowList12(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L")

      withQueryResult(schema :+ row12("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple12("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort)

      }
    }

    "be flatten from 13 columns to Tuple13" in {
      val schema = rowList13(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M")

      withQueryResult(schema :+ row13("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple13("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte)

      }
    }

    "be flatten from 14 columns to Tuple14" in {
      val schema = rowList14(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N")

      withQueryResult(schema :+ row14("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple14("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false)

      }
    }

    "be flatten from 15 columns to Tuple15" in {
      val schema = rowList15(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O")

      withQueryResult(schema :+ row15("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C")) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple15("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C")

      }
    }

    "be flatten from 16 columns to Tuple16" in {
      val schema = rowList16(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P")

      withQueryResult(schema :+ row16("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple16("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3)

      }
    }

    "be flatten from 17 columns to Tuple17" in {
      val schema = rowList17(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q")

      withQueryResult(schema :+ row17("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple17("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l)

      }
    }

    "be flatten from 18 columns to Tuple18" in {
      val schema = rowList18(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q", classOf[Double] -> "R")

      withQueryResult(schema :+ row18("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") ~ get[Double]("R") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple18("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d)

      }
    }

    "be flatten from 19 columns to Tuple19" in {
      val schema = rowList19(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q", classOf[Double] -> "R", classOf[Short] -> "S")

      withQueryResult(schema :+ row19("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") ~ get[Double]("R") ~ get[Short]("S") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple19("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort)

      }
    }

    "be flatten from 20 columns to Tuple20" in {
      val schema = rowList20(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q", classOf[Double] -> "R", classOf[Short] -> "S", classOf[String] -> "T")

      withQueryResult(schema :+ row20("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D")) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") ~ get[Double]("R") ~ get[Short]("S") ~ str("T") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple20("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D")

      }
    }

    "be flatten from 21 columns to Tuple21" in {
      val schema = rowList21(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q", classOf[Double] -> "R", classOf[Short] -> "S", classOf[String] -> "T", classOf[Int] -> "U")

      withQueryResult(schema :+ row21("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D", 4)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") ~ get[Double]("R") ~ get[Short]("S") ~ str("T") ~ int("U") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple21("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D", 4)

      }
    }

    "be flatten from 22 columns to Tuple22" in {
      val schema = rowList22(classOf[String] -> "A", classOf[Int] -> "B", classOf[Long] -> "C", classOf[Double] -> "D", classOf[Short] -> "E", classOf[Byte] -> "F", classOf[Boolean] -> "G", classOf[String] -> "H", classOf[Int] -> "I", classOf[Long] -> "J", classOf[Double] -> "K", classOf[Short] -> "L", classOf[Byte] -> "M", classOf[Boolean] -> "N", classOf[String] -> "O", classOf[Int] -> "P", classOf[Long] -> "Q", classOf[Double] -> "R", classOf[Short] -> "S", classOf[String] -> "T", classOf[Int] -> "U", classOf[Long] -> "V")

      withQueryResult(schema :+ row22("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D", 4, 5l)) { implicit c =>
        SQL("SELECT * FROM test").as(
          str("A") ~ int("B") ~ long("C") ~ get[Double]("D") ~ get[Short]("E") ~ get[Byte]("F") ~ bool("G") ~ str("H") ~ int("I") ~ long("J") ~ get[Double]("K") ~ get[Short]("L") ~ get[Byte]("M") ~ bool("N") ~ str("O") ~ int("P") ~ long("Q") ~ get[Double]("R") ~ get[Short]("S") ~ str("T") ~ int("U") ~ long("V") map (SqlParser.flatten) single).
          aka("flatten columns") must_== Tuple22("A", 2, 3l, 4.56d, 9.toShort, 10.toByte, true, "B", 3, 4l, 5.67d, 10.toShort, 11.toByte, false, "C", 3, 4l, 5.678d, 16.toShort, "D", 4, 5l)

      }
    }
  }

  def withQueryResult[A](r: acolyte.QueryResult)(f: java.sql.Connection => A): A = f(connection(handleQuery { _ => r }))

}
