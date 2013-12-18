package anorm

sealed case class TupleFlattener[F](f: F)

/** Conversions to flatten columns to tuple. */
object TupleFlattener extends TupleFlattenerPriority21
// Inheritance hack to prioritize implicit conversions

/**
 * Conversion from 2-column tuple-like to [[scala.Tuple2]],
 * with resolution priority 1.
 */
sealed trait TupleFlattenerPriority1 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   */
  implicit def flattenerTo2[T1, T2]: TupleFlattener[(T1 ~ T2) => (T1, T2)] = TupleFlattener[(T1 ~ T2) => (T1, T2)] { case (c1 ~ c2) => (c1, c2) }
}

/**
 * Conversion from 3-column tuple-like to [[scala.Tuple3]],
 * with resolution priority 2.
 */
sealed trait TupleFlattenerPriority2 extends TupleFlattenerPriority1 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   */
  implicit def flattenerTo3[T1, T2, T3]: TupleFlattener[(T1 ~ T2 ~ T3) => (T1, T2, T3)] = TupleFlattener[(T1 ~ T2 ~ T3) => (T1, T2, T3)] { case (c1 ~ c2 ~ c3) => (c1, c2, c3) }
}

/**
 * Conversion from 4-column tuple-like to [[scala.Tuple4]],
 * with resolution priority 3.
 */
sealed trait TupleFlattenerPriority3 extends TupleFlattenerPriority2 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   */
  implicit def flattenerTo4[T1, T2, T3, T4]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4) => (T1, T2, T3, T4)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4) => (T1, T2, T3, T4)] { case (c1 ~ c2 ~ c3 ~ c4) => (c1, c2, c3, c4) }
}

/**
 * Conversion from 5-column tuple-like to [[scala.Tuple5]],
 * with resolution priority 4.
 */
sealed trait TupleFlattenerPriority4 extends TupleFlattenerPriority3 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   */
  implicit def flattenerTo5[T1, T2, T3, T4, T5]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5) => (T1, T2, T3, T4, T5)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5) => (T1, T2, T3, T4, T5)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5) => (c1, c2, c3, c4, c5) }
}

/**
 * Conversion from 6-column tuple-like to [[scala.Tuple6]],
 * with resolution priority 5.
 */
sealed trait TupleFlattenerPriority5 extends TupleFlattenerPriority4 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   */
  implicit def flattenerTo6[T1, T2, T3, T4, T5, T6]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6) => (T1, T2, T3, T4, T5, T6)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6) => (T1, T2, T3, T4, T5, T6)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6) => (c1, c2, c3, c4, c5, c6) }
}

/**
 * Conversion from 7-column tuple-like to [[scala.Tuple7]],
 * with resolution priority 6.
 */
sealed trait TupleFlattenerPriority6 extends TupleFlattenerPriority5 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   */
  implicit def flattenerTo7[T1, T2, T3, T4, T5, T6, T7]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7) => (T1, T2, T3, T4, T5, T6, T7)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7) => (T1, T2, T3, T4, T5, T6, T7)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7) => (c1, c2, c3, c4, c5, c6, c7) }
}

/**
 * Conversion from 8-column tuple-like to [[scala.Tuple8]],
 * with resolution priority 7.
 */
sealed trait TupleFlattenerPriority7 extends TupleFlattenerPriority6 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   */
  implicit def flattenerTo8[T1, T2, T3, T4, T5, T6, T7, T8]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8) => (T1, T2, T3, T4, T5, T6, T7, T8)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8) => (T1, T2, T3, T4, T5, T6, T7, T8)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8) => (c1, c2, c3, c4, c5, c6, c7, c8) }
}

/**
 * Conversion from 9-column tuple-like to [[scala.Tuple9]],
 * with resolution priority 8.
 */
sealed trait TupleFlattenerPriority8 extends TupleFlattenerPriority7 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   */
  implicit def flattenerTo9[T1, T2, T3, T4, T5, T6, T7, T8, T9]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9) => (T1, T2, T3, T4, T5, T6, T7, T8, T9)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9) => (T1, T2, T3, T4, T5, T6, T7, T8, T9)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9) => (c1, c2, c3, c4, c5, c6, c7, c8, c9) }
}

/**
 * Conversion from 10-column tuple-like to [[scala.Tuple10]],
 * with resolution priority 9.
 */
sealed trait TupleFlattenerPriority9 extends TupleFlattenerPriority8 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   */
  implicit def flattenerTo10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) }
}

/**
 * Conversion from 11-column tuple-like to [[scala.Tuple11]],
 * with resolution priority 10.
 */
sealed trait TupleFlattenerPriority10 extends TupleFlattenerPriority9 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   */
  implicit def flattenerTo11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11) }
}

/**
 * Conversion from 12-column tuple-like to [[scala.Tuple12]],
 * with resolution priority 11.
 */
sealed trait TupleFlattenerPriority11 extends TupleFlattenerPriority10 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   */
  implicit def flattenerTo12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12) }
}

/**
 * Conversion from 13-column tuple-like to [[scala.Tuple13]],
 * with resolution priority 12.
 */
sealed trait TupleFlattenerPriority12 extends TupleFlattenerPriority11 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   */
  implicit def flattenerTo13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13) }
}

/**
 * Conversion from 14-column tuple-like to [[scala.Tuple14]],
 * with resolution priority 13.
 */
sealed trait TupleFlattenerPriority13 extends TupleFlattenerPriority12 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   */
  implicit def flattenerTo14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14) }
}

/**
 * Conversion from 15-column tuple-like to [[scala.Tuple15]],
 * with resolution priority 14.
 */
sealed trait TupleFlattenerPriority14 extends TupleFlattenerPriority13 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   */
  implicit def flattenerTo15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15) }
}

/**
 * Conversion from 16-column tuple-like to [[scala.Tuple16]],
 * with resolution priority 15.
 */
sealed trait TupleFlattenerPriority15 extends TupleFlattenerPriority14 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   */
  implicit def flattenerTo16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16) }
}

/**
 * Conversion from 17-column tuple-like to [[scala.Tuple17]],
 * with resolution priority 16.
 */
sealed trait TupleFlattenerPriority16 extends TupleFlattenerPriority15 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   */
  implicit def flattenerTo17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17) }
}

/**
 * Conversion from 18-column tuple-like to [[scala.Tuple18]],
 * with resolution priority 17.
 */
sealed trait TupleFlattenerPriority17 extends TupleFlattenerPriority16 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   * @param c18 Column #18
   */
  implicit def flattenerTo18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17 ~ c18) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18) }
}

/**
 * Conversion from 19-column tuple-like to [[scala.Tuple19]],
 * with resolution priority 18.
 */
sealed trait TupleFlattenerPriority18 extends TupleFlattenerPriority17 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   * @param c18 Column #18
   * @param c19 Column #19
   */
  implicit def flattenerTo19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17 ~ c18 ~ c19) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19) }
}

/**
 * Conversion from 20-column tuple-like to [[scala.Tuple20]],
 * with resolution priority 19.
 */
sealed trait TupleFlattenerPriority19 extends TupleFlattenerPriority18 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   * @param c18 Column #18
   * @param c19 Column #19
   * @param c20 Column #20
   */
  implicit def flattenerTo20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17 ~ c18 ~ c19 ~ c20) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20) }
}

/**
 * Conversion from 21-column tuple-like to [[scala.Tuple21]],
 * with resolution priority 20.
 */
sealed trait TupleFlattenerPriority20 extends TupleFlattenerPriority19 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   * @param c18 Column #18
   * @param c19 Column #19
   * @param c20 Column #20
   * @param c21 Column #21
   */
  implicit def flattenerTo21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20 ~ T21) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20 ~ T21) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17 ~ c18 ~ c19 ~ c20 ~ c21) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20, c21) }
}

/**
 * Conversion from 22-column tuple-like to [[scala.Tuple22]],
 * with resolution priority 21.
 */
sealed trait TupleFlattenerPriority21 extends TupleFlattenerPriority20 {
  /**
   * @param c1 Column #1
   * @param c2 Column #2
   * @param c3 Column #3
   * @param c4 Column #4
   * @param c5 Column #5
   * @param c6 Column #6
   * @param c7 Column #7
   * @param c8 Column #8
   * @param c9 Column #9
   * @param c10 Column #10
   * @param c11 Column #11
   * @param c12 Column #12
   * @param c13 Column #13
   * @param c14 Column #14
   * @param c15 Column #15
   * @param c16 Column #16
   * @param c17 Column #17
   * @param c18 Column #18
   * @param c19 Column #19
   * @param c20 Column #20
   * @param c21 Column #21
   * @param c22 Column #22
   */
  implicit def flattenerTo22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20 ~ T21 ~ T22) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11 ~ T12 ~ T13 ~ T14 ~ T15 ~ T16 ~ T17 ~ T18 ~ T19 ~ T20 ~ T21 ~ T22) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] { case (c1 ~ c2 ~ c3 ~ c4 ~ c5 ~ c6 ~ c7 ~ c8 ~ c9 ~ c10 ~ c11 ~ c12 ~ c13 ~ c14 ~ c15 ~ c16 ~ c17 ~ c18 ~ c19 ~ c20 ~ c21 ~ c22) => (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19, c20, c21, c22) }
}
