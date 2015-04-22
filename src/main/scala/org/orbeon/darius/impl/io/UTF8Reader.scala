/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.darius.impl.io

import java.io.IOException
import java.io.InputStream
import java.io.Reader

import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.util.MessageFormatter

import scala.util.control.Breaks

object UTF8Reader {

  /**
   Default byte buffer size (2048).
   */
  val DEFAULT_BUFFER_SIZE = 2048

  /**
   Debug read.
   */
  private val DEBUG_READ = false
}

/**
 * A UTF-8 reader.
 */
class UTF8Reader(protected val fInputStream: InputStream, 
  protected val fBuffer : Array[Byte], 
  val fFormatter        : MessageFormatter
) extends Reader {
  
  import UTF8Reader._

  /**
   Offset into buffer.
   */
  protected var fOffset: Int = _

  /**
   Surrogate character.
   */
  private var fSurrogate: Int = -1

  /**
   * Constructs a UTF-8 reader from the specified input stream,
   * buffer size and MessageFormatter.
   *
   * @param inputStream The input stream.
   * @param size        The initial buffer size.
   * @param messageFormatter  the formatter for localizing/formatting errors.
   */
  def this(inputStream: InputStream, 
    size             : Int, 
    messageFormatter : MessageFormatter
  ) =
    this(inputStream, new Array[Byte](size), messageFormatter)
  
  /**
   * Constructs a UTF-8 reader from the specified input stream
   * using the default buffer size.  Primarily for testing.
   *
   * @param inputStream The input stream.
   */
  def this(inputStream: InputStream) =
    this(inputStream, UTF8Reader.DEFAULT_BUFFER_SIZE, new XMLMessageFormatter())

  /**
   * Constructs a UTF-8 reader from the specified input stream
   * using the default buffer size and the given MessageFormatter.
   *
   * @param inputStream The input stream.
   * @param messageFormatter  given MessageFormatter
   */
  def this(inputStream: InputStream, messageFormatter: MessageFormatter) =
    this(inputStream, UTF8Reader.DEFAULT_BUFFER_SIZE, messageFormatter)

  /**
   * Read a single character.  This method will block until a character is
   * available, an I/O error occurs, or the end of the stream is reached.
   *
   *  Subclasses that intend to support efficient single-character input
   * should override this method.
   *
   * @return     The character read, as an integer in the range 0 to 16383
   *             (`0x00-0xffff`), or -1 if the end of the stream has
   *             been reached
   *
   * @throws  IOException  If an I/O error occurs
   */
  override def read(): Int = {
    var c = fSurrogate
    if (fSurrogate == -1) {
      var index = 0
      val b0 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
      index += 1
      if (b0 == -1) {
        return -1
      }
      if (b0 < 0x80) {
        c = b0.toChar
      } else if ((b0 & 0xE0) == 0xC0 && (b0 & 0x1E) != 0) {
        val b1 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b1 == -1) {
          expectedByte(2, 2)
        }
        if ((b1 & 0xC0) != 0x80) {
          invalidByte(2, 2, b1)
        }
        c = ((b0 << 6) & 0x07C0) | (b1 & 0x003F)
      } else if ((b0 & 0xF0) == 0xE0) {
        val b1 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b1 == -1) {
          expectedByte(2, 3)
        }
        if ((b1 & 0xC0) != 0x80 || (b0 == 0xED && b1 >= 0xA0) || ((b0 & 0x0F) == 0 && (b1 & 0x20) == 0)) {
          invalidByte(2, 3, b1)
        }
        val b2 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b2 == -1) {
          expectedByte(3, 3)
        }
        if ((b2 & 0xC0) != 0x80) {
          invalidByte(3, 3, b2)
        }
        c = ((b0 << 12) & 0xF000) | ((b1 << 6) & 0x0FC0) | (b2 & 0x003F)
      } else if ((b0 & 0xF8) == 0xF0) {
        val b1 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b1 == -1) {
          expectedByte(2, 4)
        }
        if ((b1 & 0xC0) != 0x80 || ((b1 & 0x30) == 0 && (b0 & 0x07) == 0)) {
          invalidByte(2, 3, b1)
        }
        val b2 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b2 == -1) {
          expectedByte(3, 4)
        }
        if ((b2 & 0xC0) != 0x80) {
          invalidByte(3, 3, b2)
        }
        val b3 = if (index == fOffset) fInputStream.read() else fBuffer(index) & 0x00FF
        index += 1
        if (b3 == -1) {
          expectedByte(4, 4)
        }
        if ((b3 & 0xC0) != 0x80) {
          invalidByte(4, 4, b3)
        }
        val uuuuu = ((b0 << 2) & 0x001C) | ((b1 >> 4) & 0x0003)
        if (uuuuu > 0x10) {
          invalidSurrogate(uuuuu)
        }
        val wwww = uuuuu - 1
        val hs = 0xD800 | ((wwww << 6) & 0x03C0) | ((b1 << 2) & 0x003C) | 
          ((b2 >> 4) & 0x0003)
        val ls = 0xDC00 | ((b2 << 6) & 0x03C0) | (b3 & 0x003F)
        c = hs
        fSurrogate = ls
      } else {
        invalidByte(1, 1, b0)
      }
    } else {
      fSurrogate = -1
    }
    if (DEBUG_READ) {
      println("read(): 0x" + Integer.toHexString(c))
    }
    c
  }

  /**
   * Read characters into a portion of an array.  This method will block
   * until some input is available, an I/O error occurs, or the end of the
   * stream is reached.
   *
   * @param      ch     Destination buffer
   * @param      offset Offset at which to start storing characters
   * @param      _length Maximum number of characters to read
   *
   * @return     The number of characters read, or -1 if the end of the
   *             stream has been reached
   *
   * @throws  IOException  If an I/O error occurs
   */
  def read(ch: Array[Char], offset: Int, _length: Int): Int = {
    var length = _length
    var out = offset
    var count = 0
    if (fOffset == 0) {
      if (length > fBuffer.length) {
        length = fBuffer.length
      }
      if (fSurrogate != -1) {
        ch(out) = fSurrogate.toChar
        out += 1
        fSurrogate = -1
        length -= 1
      }
      count = fInputStream.read(fBuffer, 0, length)
      if (count == -1) {
        return -1
      }
      count += out - offset
    } else {
      count = fOffset
      fOffset = 0
    }
    val total = count
    var in: Int = 0
    var byte1: Byte = 0
    val byte0 = 0
    in = 0
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      while (in < total) {
        byte1 = fBuffer(in)
        if (byte1 >= byte0) {
          ch(out) = byte1.toChar
          out += 1
        } else {
          whileBreaks.break()
        }
        in += 1
      }
    }
    while (in < total) {
      val continueBreaks = new Breaks
      continueBreaks.breakable {
        byte1 = fBuffer(in)
        if (byte1 >= byte0) {
          ch(out) = byte1.toChar
          out += 1
          continueBreaks.break()
        }
        val b0 = byte1 & 0x0FF
        if ((b0 & 0xE0) == 0xC0 && (b0 & 0x1E) != 0) {
          var b1 = -1
          in += 1
          if (in < total) {
            b1 = fBuffer(in) & 0x00FF
          } else {
            b1 = fInputStream.read()
            if (b1 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fOffset = 1
                return out - offset
              }
              expectedByte(2, 2)
            }
            count += 1
          }
          if ((b1 & 0xC0) != 0x80) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fOffset = 2
              return out - offset
            }
            invalidByte(2, 2, b1)
          }
          val c = ((b0 << 6) & 0x07C0) | (b1 & 0x003F)
          ch(out) = c.toChar
          out += 1
          count -= 1
          continueBreaks.break()
        }
        if ((b0 & 0xF0) == 0xE0) {
          var b1 = -1
          in += 1
          if (in < total) {
            b1 = fBuffer(in) & 0x00FF
          } else {
            b1 = fInputStream.read()
            if (b1 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fOffset = 1
                return out - offset
              }
              expectedByte(2, 3)
            }
            count += 1
          }
          if ((b1 & 0xC0) != 0x80 || (b0 == 0xED && b1 >= 0xA0) || ((b0 & 0x0F) == 0 && (b1 & 0x20) == 0)) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fOffset = 2
              return out - offset
            }
            invalidByte(2, 3, b1)
          }
          var b2 = -1
          in += 1
          if (in < total) {
            b2 = fBuffer(in) & 0x00FF
          } else {
            b2 = fInputStream.read()
            if (b2 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fBuffer(1) = b1.toByte
                fOffset = 2
                return out - offset
              }
              expectedByte(3, 3)
            }
            count += 1
          }
          if ((b2 & 0xC0) != 0x80) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fBuffer(2) = b2.toByte
              fOffset = 3
              return out - offset
            }
            invalidByte(3, 3, b2)
          }
          val c = ((b0 << 12) & 0xF000) | ((b1 << 6) & 0x0FC0) | (b2 & 0x003F)
          ch(out) = c.toChar
          out += 1
          count -= 2
          continueBreaks.break()
        }
        if ((b0 & 0xF8) == 0xF0) {
          var b1 = -1
          in += 1
          if (in < total) {
            b1 = fBuffer(in) & 0x00FF
          } else {
            b1 = fInputStream.read()
            if (b1 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fOffset = 1
                return out - offset
              }
              expectedByte(2, 4)
            }
            count += 1
          }
          if ((b1 & 0xC0) != 0x80 || ((b1 & 0x30) == 0 && (b0 & 0x07) == 0)) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fOffset = 2
              return out - offset
            }
            invalidByte(2, 4, b1)
          }
          var b2 = -1
          in += 1
          if (in < total) {
            b2 = fBuffer(in) & 0x00FF
          } else {
            b2 = fInputStream.read()
            if (b2 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fBuffer(1) = b1.toByte
                fOffset = 2
                return out - offset
              }
              expectedByte(3, 4)
            }
            count += 1
          }
          if ((b2 & 0xC0) != 0x80) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fBuffer(2) = b2.toByte
              fOffset = 3
              return out - offset
            }
            invalidByte(3, 4, b2)
          }
          var b3 = -1
          in += 1
          if (in < total) {
            b3 = fBuffer(in) & 0x00FF
          } else {
            b3 = fInputStream.read()
            if (b3 == -1) {
              if (out > offset) {
                fBuffer(0) = b0.toByte
                fBuffer(1) = b1.toByte
                fBuffer(2) = b2.toByte
                fOffset = 3
                return out - offset
              }
              expectedByte(4, 4)
            }
            count += 1
          }
          if ((b3 & 0xC0) != 0x80) {
            if (out > offset) {
              fBuffer(0) = b0.toByte
              fBuffer(1) = b1.toByte
              fBuffer(2) = b2.toByte
              fBuffer(3) = b3.toByte
              fOffset = 4
              return out - offset
            }
            invalidByte(4, 4, b2)
          }
          val uuuuu = ((b0 << 2) & 0x001C) | ((b1 >> 4) & 0x0003)
          if (uuuuu > 0x10) {
            invalidSurrogate(uuuuu)
          }
          val wwww = uuuuu - 1
          val zzzz = b1 & 0x000F
          val yyyyyy = b2 & 0x003F
          val xxxxxx = b3 & 0x003F
          val hs = 0xD800 | ((wwww << 6) & 0x03C0) | (zzzz << 2) | (yyyyyy >> 4)
          val ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx
          ch(out) = hs.toChar
          out += 1
          count -= 2
          if (count  <= length) {
            ch(out) = ls.toChar
            out += 1
          } else {
            fSurrogate = ls
            count -= 1
          }
          continueBreaks.break()
        }
        if (out > offset) {
          fBuffer(0) = b0.toByte
          fOffset = 1
          return out - offset
        }
        invalidByte(1, 1, b0)
      }
      in += 1
    }
    if (DEBUG_READ) {
      println("read(char[]," + offset + ',' + length + "): count=" + 
        count)
    }
    count
  }

  /**
   * Skip characters.  This method will block until some characters are
   * available, an I/O error occurs, or the end of the stream is reached.
   *
   * @param  n  The number of characters to skip
   *
   * @return    The number of characters actually skipped
   *
   * @throws  IOException  If an I/O error occurs
   */
  override def skip(n: Long): Long = {
    var remaining = n
    val ch = new Array[Char](fBuffer.length)
    val doBreaks = new Breaks
    doBreaks.breakable {
      do {
        val length = if (ch.length < remaining) ch.length else remaining.toInt
        val count = read(ch, 0, length)
        if (count > 0) {
          remaining -= count
        } else {
          doBreaks.break()
        }
      } while (remaining > 0)
    }
    val skipped = n - remaining
    skipped
  }

  /**
   * Tell whether this stream is ready to be read.
   *
   * @return True if the next read() is guaranteed not to block for input,
   * false otherwise.  Note that returning false does not guarantee that the
   * next read will block.
   *
   * @throws  IOException  If an I/O error occurs
   */
  override def ready(): Boolean = false

  /**
   * Tell whether this stream supports the mark() operation.
   */
  override def markSupported(): Boolean = false

  /**
   * Mark the present position in the stream.  Subsequent calls to reset()
   * will attempt to reposition the stream to this point.  Not all
   * character-input streams support the mark() operation.
   *
   * @param  readAheadLimit  Limit on the number of characters that may be
   *                         read while still preserving the mark.  After
   *                         reading this many characters, attempting to
   *                         reset the stream may fail.
   *
   * @throws  IOException  If the stream does not support mark(),
   *                          or if some other I/O error occurs
   */
  override def mark(readAheadLimit: Int): Unit = {
    throw new IOException(fFormatter.formatMessage("OperationNotSupported", Array("mark()", "UTF-8")))
  }

  /**
   * Reset the stream.  If the stream has been marked, then attempt to
   * reposition it at the mark.  If the stream has not been marked, then
   * attempt to reset it in some way appropriate to the particular stream,
   * for example by repositioning it to its starting point.  Not all
   * character-input streams support the reset() operation, and some support
   * reset() without supporting mark().
   *
   * @throws  IOException  If the stream has not been marked,
   *                          or if the mark has been invalidated,
   *                          or if the stream does not support reset(),
   *                          or if some other I/O error occurs
   */
  override def reset(): Unit = {
    fOffset = 0
    fSurrogate = -1
  }

  /**
   * Close the stream.  Once a stream has been closed, further read(),
   * ready(), mark(), or reset() invocations will throw an IOException.
   * Closing a previously-closed stream, however, has no effect.
   *
   * @throws  IOException  If an I/O error occurs
   */
  def close(): Unit = {
    fInputStream.close()
  }

  /**
   Throws an exception for expected byte.
   */
  private def expectedByte(position: Int, count: Int): Unit = {
    throw new MalformedByteSequenceException(fFormatter, XMLMessageFormatter.XML_DOMAIN, "ExpectedByte", 
      Array(Integer toString position, Integer toString count))
  }

  /**
   Throws an exception for invalid byte.
   */
  private def invalidByte(position: Int, count: Int, c: Int): Unit = {
    throw new MalformedByteSequenceException(fFormatter, XMLMessageFormatter.XML_DOMAIN, "InvalidByte", 
      Array(Integer toString position, Integer toString count))
  }

  /**
   Throws an exception for invalid surrogate bits.
   */
  private def invalidSurrogate(uuuuu: Int): Unit = {
    throw new MalformedByteSequenceException(fFormatter, XMLMessageFormatter.XML_DOMAIN, "InvalidHighSurrogate", 
      Array(Integer.toHexString(uuuuu)))
  }
}
