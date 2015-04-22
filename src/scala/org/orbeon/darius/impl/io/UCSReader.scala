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

import scala.util.control.Breaks

object UCSReader {

  /**
   * Default byte buffer size (8192, larger than that of ASCIIReader
   * since it's reasonable to surmise that the average UCS-4-encoded
   * file should be 4 times as large as the average ASCII-encoded file).
   */
  val DEFAULT_BUFFER_SIZE = 8192

  val UCS2LE: Short = 1
  val UCS2BE: Short = 2
  val UCS4LE: Short = 4
  val UCS4BE: Short = 8
}

/**
 * Reader for UCS-2 and UCS-4 encodings.
 * (i.e., encodings from ISO-10646-UCS-(2|4)).
 */
class UCSReader(protected val fInputStream: InputStream, protected val fBuffer: Array[Byte], protected val fEncoding: Short)
    extends Reader {
  
  import UCSReader._

  /**
   * Constructs a UCS reader from the specified input stream
   * and buffer size.  The Endian-ness and whether this is
   * UCS-2 or UCS-4 needs also to be known in advance.
   *
   * @param inputStream The input stream.
   * @param size        The initial buffer size.
   * @param encoding One of UCS2LE, UCS2BE, UCS4LE or UCS4BE.
   */
  def this(inputStream: InputStream, size: Int, encoding: Short) {
    this(inputStream, new Array[Byte](size), encoding)
  }
  
  /**
   * Constructs a UCS reader from the specified input stream
   * using the default buffer size.  The Endian-ness and whether this is
   * UCS-2 or UCS-4 needs also to be known in advance.
   *
   * @param inputStream The input stream.
   * @param encoding One of UCS2LE, UCS2BE, UCS4LE or UCS4BE.
   */
  def this(inputStream: InputStream, encoding: Short) {
    this(inputStream, UCSReader.DEFAULT_BUFFER_SIZE, encoding)
  }

  /**
   * Read a single character.  This method will block until a character is
   * available, an I/O error occurs, or the end of the stream is reached.
   *
   *  Subclasses that intend to support efficient single-character input
   * should override this method.
   *
   * @return     The character read, as an integer in the range 0 to 127
   *             (`0x00-0x7f`), or -1 if the end of the stream has
   *             been reached
   *
   * @throws  IOException  If an I/O error occurs
   */
  override def read(): Int = {
    val b0 = fInputStream.read() & 0xff
    if (b0 == 0xff) {
      return -1
    }
    val b1 = fInputStream.read() & 0xff
    if (b1 == 0xff) {
      return -1
    }
    if (fEncoding >= 4) {
      val b2 = fInputStream.read() & 0xff
      if (b2 == 0xff) {
        return -1
      }
      val b3 = fInputStream.read() & 0xff
      if (b3 == 0xff) {
        return -1
      }
      if (fEncoding == UCS4BE) {
        return (b0 << 24) + (b1 << 16) + (b2 << 8) + b3
      }
      return (b3 << 24) + (b2 << 16) + (b1 << 8) + b0
    }
    if (fEncoding == UCS2BE) {
      return (b0 << 8) + b1
    }
    (b1 << 8) + b0
  }

  /**
   * Read characters into a portion of an array.  This method will block
   * until some input is available, an I/O error occurs, or the end of the
   * stream is reached.
   *
   * @param      ch     Destination buffer
   * @param      offset Offset at which to start storing characters
   * @param      length Maximum number of characters to read
   *
   * @return     The number of characters read, or -1 if the end of the
   *             stream has been reached
   *
   * @throws  IOException  If an I/O error occurs
   */
  def read(ch: Array[Char], offset: Int, length: Int): Int = {
    var byteLength = length << (if (fEncoding >= 4) 2 else 1)
    if (byteLength > fBuffer.length) {
      byteLength = fBuffer.length
    }
    var count = fInputStream.read(fBuffer, 0, byteLength)
    if (count == -1) return -1
    if (fEncoding >= 4) {
      val numToRead = 4 - (count & 3) & 3
      val forBreaks = new Breaks
      forBreaks.breakable {
        for (i ← 0 until numToRead) {
          val charRead = fInputStream.read()
          if (charRead == -1) {
            for (j ← i until numToRead) {
              fBuffer(count + j) = 0
            }
            forBreaks.break()
          }
          fBuffer(count + i) = charRead.toByte
        }
      }
      count += numToRead
    } else {
      val numToRead = count & 1
      if (numToRead != 0) {
        count += 1
        val charRead = fInputStream.read()
        fBuffer(count) = if (charRead == -1) 0 else charRead.toByte
      }
    }
    val numChars = count >> (if (fEncoding >= 4) 2 else 1)
    var curPos = 0
    for (i ← 0 until numChars) {
      val b0 = fBuffer(curPos) & 0xff
      curPos += 1
      val b1 = fBuffer(curPos) & 0xff
      curPos += 1
      if (fEncoding >= 4) {
        val b2 = fBuffer(curPos) & 0xff
        curPos += 1
        val b3 = fBuffer(curPos) & 0xff
        curPos += 1
        ch(offset + i) = if (fEncoding == UCS4BE) ((b0 << 24) + (b1 << 16) + (b2 << 8) + b3).toChar else ((b3 << 24) + (b2 << 16) + (b1 << 8) + b0).toChar
      } else {
        ch(offset + i) = if (fEncoding == UCS2BE) ((b0 << 8) + b1).toChar else ((b1 << 8) + b0).toChar
      }
    }
    numChars
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
    val charWidth = if (fEncoding >= 4) 2 else 1
    val bytesSkipped = fInputStream.skip(n << charWidth)
    if ((bytesSkipped & (charWidth | 1)) == 0) return bytesSkipped >> charWidth
    (bytesSkipped >> charWidth) + 1
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
  override def markSupported(): Boolean = fInputStream.markSupported()

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
    fInputStream.mark(readAheadLimit)
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
    fInputStream.reset()
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
}
