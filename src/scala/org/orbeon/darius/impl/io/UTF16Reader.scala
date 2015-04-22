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

import org.orbeon.darius.impl.io.UTF16Reader._
import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.util.MessageFormatter

object UTF16Reader {

  /**
   Default byte buffer size (4096).
   */
  val DEFAULT_BUFFER_SIZE = 4096
}

/**
 * A UTF-16 reader. Can also be used for UCS-2 (i.e. ISO-10646-UCS-2).
 */
class UTF16Reader(protected val fInputStream: InputStream, 
  protected val fBuffer      : Array[Byte], 
  protected val fIsBigEndian : Boolean, 
  val fFormatter             : MessageFormatter
) extends Reader {

  /**
   * Constructs a UTF-16 reader from the specified input stream
   * and buffer size and given MessageFormatter.
   *
   * @param inputStream       The input stream.
   * @param size              The initial buffer size.
   * @param isBigEndian       The byte order.
   * @param messageFormatter  Given MessageFormatter
   */
  def this(inputStream: InputStream, 
      size             : Int, 
      isBigEndian      : Boolean, 
      messageFormatter : MessageFormatter
  ) =
    this(inputStream, new Array[Byte](size), isBigEndian, messageFormatter)
  
  /**
   * Constructs a UTF-16 reader from the specified input stream
   * using the default buffer size. Primarily for testing.
   *
   * @param inputStream The input stream.
   * @param isBigEndian The byte order.
   */
  def this(inputStream: InputStream, isBigEndian: Boolean) =
    this(inputStream, DEFAULT_BUFFER_SIZE, isBigEndian, new XMLMessageFormatter())

  /**
   * Constructs a UTF-16 reader from the specified input stream
   * using the default buffer size and the given MessageFormatter.
   *
   * @param inputStream The input stream.
   * @param isBigEndian The byte order.
   */
  def this(inputStream : InputStream, 
    isBigEndian        : Boolean, 
    messageFormatter   : MessageFormatter
  ) =
    this(inputStream, DEFAULT_BUFFER_SIZE, isBigEndian, messageFormatter)

  /**
   * Read a single character.  This method will block until a character is
   * available, an I/O error occurs, or the end of the stream is reached.
   *
   *  Subclasses that intend to support efficient single-character input
   * should override this method.
   *
   * @return     The character read, as an integer in the range 0 to 65535
   *             (`0x00-0xffff`), or -1 if the end of the stream has
   *             been reached
   *
   * @throws  IOException  If an I/O error occurs
   */
  override def read(): Int = {
    val b0 = fInputStream.read()
    if (b0 == -1) {
      return -1
    }
    val b1 = fInputStream.read()
    if (b1 == -1) {
      expectedTwoBytes()
    }
    if (fIsBigEndian) {
      return (b0 << 8) | b1
    }
    (b1 << 8) | b0
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
    var byteLength = length << 1
    if (byteLength > fBuffer.length) {
      byteLength = fBuffer.length
    }
    var byteCount = fInputStream.read(fBuffer, 0, byteLength)
    if (byteCount == -1) {
      return -1
    }
    if ((byteCount & 1) != 0) {
      val b = fInputStream.read()
      if (b == -1) {
        expectedTwoBytes()
      }
      fBuffer(byteCount) = b.toByte
      byteCount += 1
    }
    val charCount = byteCount >> 1
    if (fIsBigEndian) {
      processBE(ch, offset, charCount)
    } else {
      processLE(ch, offset, charCount)
    }
    charCount
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
    var bytesSkipped = fInputStream.skip(n << 1)
    if ((bytesSkipped & 1) != 0) {
      val b = fInputStream.read()
      if (b == -1) {
        expectedTwoBytes()
      }
      bytesSkipped += 1
    }
    bytesSkipped >> 1
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
    throw new IOException(fFormatter.formatMessage("OperationNotSupported", Array("mark()", "UTF-16")))
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
   Decodes UTF-16BE *
   */
  private def processBE(ch: Array[Char], _offset: Int, count: Int): Unit = {
    var offset = _offset
    var curPos = 0
    for (i ← 0 until count) {
      val b0 = fBuffer(curPos ) & 0xff
      curPos += 1
      val b1 = fBuffer(curPos) & 0xff
      curPos += 1
      ch(offset) = ((b0 << 8) | b1).toChar
      offset += 1
    }
  }

  /**
   Decodes UTF-16LE *
   */
  private def processLE(ch: Array[Char], _offset: Int, count: Int): Unit = {
    var offset = _offset
    var curPos = 0
    for (i ← 0 until count) {
      val b0 = fBuffer(curPos) & 0xff
      curPos += 1
      val b1 = fBuffer(curPos) & 0xff
      curPos += 1
      ch(offset) = ((b1 << 8) | b0).toChar
      offset += 1
    }
  }

  /**
   Throws an exception for expected byte.
   */
  private def expectedTwoBytes(): Unit = {
    throw new MalformedByteSequenceException(fFormatter, XMLMessageFormatter.XML_DOMAIN, "ExpectedByte", 
      Array("2", "2"))
  }
}
