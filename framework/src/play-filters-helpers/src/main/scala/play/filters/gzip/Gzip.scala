/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.gzip

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumeratee.CheckDone
import java.util.zip._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Enumeratees for dealing with gzip streams
 */
object Gzip {
  private type Bytes = Array[Byte]
  private type CheckDoneBytes = CheckDone[Bytes, Bytes]
  private val GzipMagic = 0x8b1f
  // Gzip flags
  private val HeaderCrc = 2
  private val ExtraField = 4
  private val FileName = 8
  private val FileComment = 16

  /**
   * Create a gzip enumeratee.
   *
   * This enumeratee is not purely functional, it uses the high performance native deflate implementation provided by
   * Java, which is stateful.  However, this state is created each time the enumeratee is applied, so it is fine to
   * reuse the enumeratee returned by this function.
   *
   * @param bufferSize The size of the output buffer
   */
  def gzip(bufferSize: Int = 512): Enumeratee[Array[Byte], Array[Byte]] = {

    /*
     * State consists of 4 parts, a deflater (high performance native zlib implementation), a crc32 calculator, required
     * by gzip to be at the end of the stream, a buffer in which we accumulate the compressed bytes, and the current
     * position of that buffer.
     */
    class State {
      val deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true)
      val crc = new CRC32
      @volatile var buffer = new Bytes(bufferSize)
      @volatile var pos = 0

      def reset() {
        pos = 0
        buffer = new Bytes(bufferSize)
      }
    }

    new CheckDoneBytes {

      def step[A](state: State, k: K[Bytes, A]): K[Bytes, Iteratee[Bytes, A]] = {
        case Input.EOF => {
          state.deflater.finish()
          deflateUntilFinished(state, k)
        }

        case Input.El(bytes) => {
          state.crc.update(bytes)
          state.deflater.setInput(bytes)
          deflateUntilNeedsInput(state, k)
        }

        case in @ Input.Empty => feedEmpty(state, k)
      }

      def continue[A](k: K[Bytes, A]) = {
        feedHeader(k).pureFlatFold {
          case Step.Cont(k2) => Cont(step(new State, k2))
          case step => Done(step.it, Input.Empty)
        }
      }

      def deflateUntilNeedsInput[A](state: State, k: K[Bytes, A]): Iteratee[Bytes, Iteratee[Bytes, A]] = {
        // Deflate some bytes
        val numBytes = state.deflater.deflate(state.buffer, state.pos, bufferSize - state.pos)
        if (numBytes == 0) {
          if (state.deflater.needsInput()) {
            // Deflater needs more input, so continue
            Cont(step(state, k))
          } else {
            deflateUntilNeedsInput(state, k)
          }
        } else {
          state.pos += numBytes
          if (state.pos < bufferSize) {
            deflateUntilNeedsInput(state, k)
          } else {
            // We've filled our buffer, feed it into the k function
            val buffer = state.buffer
            state.reset()
            new CheckDoneBytes {
              def continue[B](k: K[Bytes, B]) = deflateUntilNeedsInput(state, k)
            } &> k(Input.El(buffer))
          }
        }
      }

      def deflateUntilFinished[A](state: State, k: K[Bytes, A]): Iteratee[Bytes, Iteratee[Bytes, A]] = {
        val numBytes = state.deflater.deflate(state.buffer, state.pos, bufferSize - state.pos)
        if (numBytes == 0) {
          if (state.deflater.finished()) {
            // Deflater is finished, send the trailer
            feedTrailer(state, k)
          } else {
            deflateUntilFinished(state, k)
          }
        } else {
          state.pos += numBytes
          if (state.pos < bufferSize) {
            deflateUntilFinished(state, k)
          } else {
            val buffer = state.buffer
            state.reset()
            new CheckDoneBytes {
              def continue[B](k: K[Bytes, B]) = deflateUntilFinished(state, k)
            } &> k(Input.El(buffer))
          }
        }
      }

      def feedEmpty[A](state: State, k: K[Bytes, A]) = new CheckDoneBytes {
        def continue[B](k: K[Bytes, B]) = Cont(step(state, k))
      } &> k(Input.Empty)

      def feedHeader[A](k: K[Bytes, A]) = {
        // First need to write the Gzip header
        val zero = 0.asInstanceOf[Byte]
        val header = Array(
          GzipMagic.asInstanceOf[Byte], // Magic number (2 bytes)
          (GzipMagic >> 8).asInstanceOf[Byte],
          Deflater.DEFLATED.asInstanceOf[Byte], // Compression method
          zero, // Flags
          zero, // Modification time (4 bytes)
          zero,
          zero,
          zero,
          zero, // Extra flags
          zero // Operating system
        )
        k(Input.El(header))
      }

      def feedTrailer[A](state: State, k: K[Bytes, A]): Iteratee[Bytes, Iteratee[Bytes, A]] = {
        def writeTrailer(buffer: Bytes, pos: Int) {
          val crc = state.crc.getValue
          val length = state.deflater.getTotalIn
          state.deflater.end()
          // CRC followed by length, little endian
          intToLittleEndian(crc.asInstanceOf[Int], buffer, pos)
          intToLittleEndian(length, buffer, pos + 4)
        }

        // Try to just append to the existing buffer if there's enough room
        val finalIn = if (state.pos + 8 <= bufferSize) {
          writeTrailer(state.buffer, state.pos)
          state.pos = state.pos + 8
          val buffer = if (state.pos == bufferSize) state.buffer else state.buffer.take(state.pos)
          Seq(buffer)
        } else {
          // Create a new buffer for the trailer
          val buffer = if (state.pos == bufferSize) state.buffer else state.buffer.take(state.pos)
          val trailer = new Bytes(8)
          writeTrailer(trailer, 0)
          Seq(buffer, trailer)
        }
        Iteratee.flatten(Enumerator.enumerate(finalIn) >>> Enumerator.eof |>> Cont(k)).map(it => Done(it, Input.EOF))
      }
    }
  }

  /**
   * Create a gunzip enumeratee.
   *
   * This enumeratee is not purely functional, it uses the high performance native deflate implementation provided by
   * Java, which is stateful.  However, this state is created each time the enumeratee is applied, so it is fine to
   * reuse the enumeratee returned by this function.
   *
   * @param bufferSize The size of the output buffer
   */
  def gunzip(bufferSize: Int = 512): Enumeratee[Array[Byte], Array[Byte]] = {

    /*
     * State consists of 4 parts, an inflater (high performance native zlib implementation), a crc32 calculator, required
     * by gzip to be at the end of the stream, a buffer in which we accumulate the compressed bytes, and the current
     * position of that buffer.
     */
    class State {
      val inflater = new Inflater(true)
      val crc = new CRC32
      @volatile var buffer = new Bytes(bufferSize)
      @volatile var pos = 0

      def reset() {
        pos = 0
        buffer = new Bytes(bufferSize)
      }
    }

    case class Header(magic: Short, compressionMethod: Byte, flags: Byte) {
      def hasCrc = (flags & HeaderCrc) == HeaderCrc
      def hasExtraField = (flags & ExtraField) == ExtraField
      def hasFilename = (flags & FileName) == FileName
      def hasComment = (flags & FileComment) == FileComment
    }

    new CheckDoneBytes {

      def step[A](state: State, k: K[Bytes, A]): K[Bytes, Iteratee[Bytes, A]] = {
        case Input.EOF => {
          Error("Premature end of gzip stream", Input.EOF)
        }

        case Input.El(bytes) => {
          state.inflater.setInput(bytes)
          inflateUntilNeedsInput(state, k, bytes)
        }

        case in @ Input.Empty => feedEmpty(state, k)
      }

      def continue[A](k: K[Bytes, A]) = {
        for {
          state <- readHeader
          step <- Cont(step(state, k))
        } yield step
      }

      def maybeEmpty(bytes: Bytes) = if (bytes.isEmpty) Input.Empty else Input.El(bytes)

      def inflateUntilNeedsInput[A](state: State, k: K[Bytes, A], input: Bytes): Iteratee[Bytes, Iteratee[Bytes, A]] = {
        // Inflate some bytes
        val numBytes = state.inflater.inflate(state.buffer, state.pos, bufferSize - state.pos)
        if (numBytes == 0) {
          if (state.inflater.finished()) {
            // Feed the current buffer
            val buffer = if (state.buffer.length > state.pos) {
              state.buffer.take(state.pos)
            } else {
              state.buffer
            }
            state.crc.update(buffer)
            new CheckDoneBytes {
              def continue[B](k: K[Bytes, B]) = finish(state, k, input)
            } &> k(Input.El(buffer))

          } else if (state.inflater.needsInput()) {
            // Inflater needs more input, so continue
            Cont(step(state, k))
          } else {
            inflateUntilNeedsInput(state, k, input)
          }
        } else {
          state.pos += numBytes
          if (state.pos < bufferSize) {
            inflateUntilNeedsInput(state, k, input)
          } else {
            // We've filled our buffer, feed it into the k function
            val buffer = state.buffer
            state.crc.update(buffer)
            state.reset()
            new CheckDoneBytes {
              def continue[B](k: K[Bytes, B]) = inflateUntilNeedsInput(state, k, input)
            } &> k(Input.El(buffer))
          }
        }
      }

      def feedEmpty[A](state: State, k: K[Bytes, A]) = new CheckDoneBytes {
        def continue[B](k: K[Bytes, B]) = Cont(step(state, k))
      } &> k(Input.Empty)

      def done[A](a: A = Unit): Iteratee[Bytes, A] = Done[Bytes, A](a)

      def finish[A](state: State, k: K[Bytes, A], input: Bytes): Iteratee[Bytes, Iteratee[Bytes, A]] = {
        // Get the left over bytes from the inflater
        val leftOver = if (input.length > state.inflater.getRemaining) {
          Done(Unit, Input.El(input.takeRight(state.inflater.getRemaining)))
        } else {
          done()
        }

        // Read the trailer, before sending an EOF
        for {
          _ <- leftOver
          _ <- readTrailer(state)
          done <- Done(k(Input.EOF), Input.EOF)
        } yield done
      }

      def readHeader: Iteratee[Bytes, State] = {
        // Parse header
        val crc = new CRC32
        for {
          headerBytes <- take(10, "Not enough bytes for gzip file", crc)
          header <- done(Header(littleEndianToShort(headerBytes), headerBytes(2), headerBytes(3)))
          _ <- if (header.magic != GzipMagic.asInstanceOf[Short]) Error("Not a gzip file, found header" + headerBytes.take(2).map(b => "%02X".format(b)).mkString("(", ", ", ")"), Input.El(headerBytes)) else done()
          _ <- if (header.compressionMethod != Deflater.DEFLATED) Error("Unsupported compression method", Input.El(headerBytes)) else done()
          efLength <- if (header.hasExtraField) readShort(crc) else done(0)
          _ <- if (header.hasExtraField) drop(efLength, "Not enough bytes for extra field", crc) else done()
          _ <- if (header.hasFilename) dropWhileIncluding(_ != 0x00, "EOF found in middle of file name", crc) else done()
          _ <- if (header.hasComment) dropWhileIncluding(_ != 0x00, "EOF found in middle of comment", crc) else done()
          headerCrc <- if (header.hasCrc) readShort(new CRC32) else done(0)
          _ <- if (header.hasCrc && (crc.getValue & 0xffff) != headerCrc) Error[Bytes]("Header CRC failed", Input.Empty) else done()
        } yield new State()
      }

      /**
       * Read and validate the trailer.  Returns a done iteratee if the trailer is valid, or error if not.
       */
      def readTrailer(state: State): Iteratee[Bytes, Unit] = {
        val dummy = new CRC32
        for {
          crc <- readInt("Premature EOF before gzip CRC", dummy)
          _ <- if (crc != state.crc.getValue.asInstanceOf[Int]) Error("CRC failed, was %X, expected %X".format(state.crc.getValue.asInstanceOf[Int], crc), Input.El(intToLittleEndian(crc))) else done()
          length <- readInt("Premature EOF before gzip total length", dummy)
          _ <- if (length != state.inflater.getTotalOut) Error("Length check failed", Input.El(intToLittleEndian(length))) else done()
        } yield {
          state.inflater.end()
          done()
        }
      }

      def readShort(crc: CRC32): Iteratee[Bytes, Int] = for {
        bytes <- take(2, "Not enough bytes for extra field length", crc)
      } yield {
        littleEndianToShort(bytes)
      }

      def readInt(error: String, crc: CRC32): Iteratee[Bytes, Int] = for {
        bytes <- take(4, error, crc)
      } yield {
        littleEndianToInt(bytes)
      }

      def take(n: Int, error: String, crc: CRC32, bytes: Bytes = new Bytes(0)): Iteratee[Bytes, Bytes] = Cont {
        case Input.EOF => Error(error, Input.EOF)
        case Input.Empty => take(n, error, crc, bytes)
        case Input.El(b) => {
          val splitted = b.splitAt(n)
          crc.update(splitted._1)
          splitted match {
            case (needed, left) if needed.length == n => Done(bytes ++ needed, maybeEmpty(left))
            case (partial, _) => take(n - partial.length, error, crc, bytes ++ partial)
          }
        }
      }

      def drop(n: Int, error: String, crc: CRC32): Iteratee[Bytes, Unit] = Cont {
        case Input.EOF => Error(error, Input.EOF)
        case Input.Empty => drop(n, error, crc)
        case Input.El(b) => if (b.length >= n) {
          val splitted = b.splitAt(n)
          crc.update(splitted._1)
          Done(Unit, maybeEmpty(splitted._2))
        } else {
          crc.update(b)
          drop(b.length - n, error, crc)
        }
      }

      def dropWhileIncluding(p: Byte => Boolean, error: String, crc: CRC32): Iteratee[Bytes, Unit] = Cont {
        case Input.EOF => Error(error, Input.EOF)
        case Input.Empty => dropWhileIncluding(p, error, crc)
        case Input.El(b) =>
          val left = b.dropWhile(p)
          crc.update(b, 0, b.length - left.length)
          left match {
            case none if none.isEmpty => dropWhileIncluding(p, error, crc)
            case some => Done(Unit, maybeEmpty(some.drop(1)))
          }
      }
    }
  }

  private def intToLittleEndian(i: Int, out: Bytes = new Bytes(4), offset: Int = 0): Bytes = {
    out(offset) = (i & 0xff).asInstanceOf[Byte]
    out(offset + 1) = (i >> 8 & 0xff).asInstanceOf[Byte]
    out(offset + 2) = (i >> 16 & 0xff).asInstanceOf[Byte]
    out(offset + 3) = (i >> 24 & 0xff).asInstanceOf[Byte]
    out
  }

  private def littleEndianToShort(bytes: Bytes, offset: Int = 0): Short = {
    ((bytes(offset + 1) & 0xff) << 8 | bytes(offset) & 0xff).asInstanceOf[Short]
  }

  private def littleEndianToInt(bytes: Bytes, offset: Int = 0): Int = {
    (bytes(offset + 3) & 0xff) << 24 |
      (bytes(offset + 2) & 0xff) << 16 |
      (bytes(offset + 1) & 0xff) << 8 |
      (bytes(offset) & 0xff)
  }
}

