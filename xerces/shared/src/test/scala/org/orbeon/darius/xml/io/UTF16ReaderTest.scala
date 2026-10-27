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

package org.orbeon.darius.xml.io

import java.io.{ByteArrayInputStream, IOException, InputStream, Reader}
import org.orbeon.apache.xerces.impl.io.{MalformedByteSequenceException, UTF16Reader}
import org.orbeon.apache.xerces.util.XMLChar
import utest._

/**
 * Converted from Xerces Java test suite: `tests/io/UTF16.java`.
 *
 * Tests the UTF-16 reader for both Big Endian (BE) and Little Endian (LE) byte orders,
 * character-by-character, in block reads, and with malformed byte sequences.
 */
object UTF16ReaderTest extends TestSuite {

  private val BlockReadSize = 2048

  override def tests = Tests {

    test("UTF16Reader Big Endian character by character over full Unicode range") {
      val stream = new UTF16Producer(isBigEndian = true)
      val reader = new UTF16Reader(stream, isBigEndian = true)
      try {
        testCharByChar(reader)
      } finally {
        reader.close()
      }
    }

    test("UTF16Reader Little Endian character by character over full Unicode range") {
      val stream = new UTF16Producer(isBigEndian = false)
      val reader = new UTF16Reader(stream, isBigEndian = false)
      try {
        testCharByChar(reader)
      } finally {
        reader.close()
      }
    }

    test("UTF16Reader Big Endian block reads (2048 chars)") {
      val stream = new UTF16Producer(isBigEndian = true)
      val reader = new UTF16Reader(stream, isBigEndian = true)
      try {
        testCharArray(reader, BlockReadSize)
      } finally {
        reader.close()
      }
    }

    test("UTF16Reader Little Endian block reads (2048 chars)") {
      val stream = new UTF16Producer(isBigEndian = false)
      val reader = new UTF16Reader(stream, isBigEndian = false)
      try {
        testCharArray(reader, BlockReadSize)
      } finally {
        reader.close()
      }
    }

    test("UTF16Reader small block reads (17 chars)") {
      val streamBE = new UTF16Producer(isBigEndian = true)
      val readerBE = new UTF16Reader(streamBE, isBigEndian = true)
      try {
        testCharArray(readerBE, 17)
      } finally {
        readerBE.close()
      }

      val streamLE = new UTF16Producer(isBigEndian = false)
      val readerLE = new UTF16Reader(streamLE, isBigEndian = false)
      try {
        testCharArray(readerLE, 17)
      } finally {
        readerLE.close()
      }
    }

    test("UTF16Reader rejects odd byte count in char-by-char read") {
      val in = new ByteArrayInputStream(Array(0x00.toByte, 0x41.toByte, 0x00.toByte))
      val reader = new UTF16Reader(in, isBigEndian = true)
      assert(reader.read() == 'A'.toInt)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }

    test("UTF16Reader rejects odd byte count in block read") {
      val in = new ByteArrayInputStream(Array(0x00.toByte, 0x41.toByte, 0x00.toByte))
      val reader = new UTF16Reader(in, isBigEndian = true)
      val ch = new Array[Char](10)
      intercept[MalformedByteSequenceException] {
        reader.read(ch, 0, ch.length)
      }
    }
  }

  private def testCharByChar(reader: Reader): Unit = {
    // 0x000000 -> 0x00D7FF
    var i = 0
    while (i < 0xD800) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x00E000 -> 0x00FFFD (skip FFFE)
    i = 0xE000
    while (i < 0xFFFE) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x010000 -> 0x10FFFF
    i = 0x10000
    while (i < 0x110000) {
      val hs = XMLChar.highSurrogate(i).toInt
      val ls = XMLChar.lowSurrogate(i).toInt

      val c1 = reader.read()
      if (c1 != hs) expectedChar("high surrogate", hs, c1)

      val c2 = reader.read()
      if (c2 != ls) expectedChar("low surrogate", ls, c2)

      i += 1
    }

    val end = reader.read()
    if (end != -1) extraChar(end)
  }

  private def testCharArray(reader: Reader, size: Int): Unit = {
    val ch = new Array[Char](size)
    var count = 0
    var position = 0

    def nextChar(): Int = {
      if (position == count) {
        count = reader.read(ch, 0, ch.length)
        position = 0
      }
      if (count == -1) -1
      else {
        val c = ch(position)
        position += 1
        c.toInt
      }
    }

    // 0x000000 -> 0x00D7FF
    var i = 0
    while (i < 0xD800) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x00E000 -> 0x00FFFD
    i = 0xE000
    while (i < 0xFFFE) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x010000 -> 0x10FFFF
    i = 0x10000
    while (i < 0x110000) {
      val hs = XMLChar.highSurrogate(i).toInt
      val ls = XMLChar.lowSurrogate(i).toInt

      val c1 = nextChar()
      if (c1 != hs) expectedChar("high surrogate", hs, c1)

      val c2 = nextChar()
      if (c2 != ls) expectedChar("low surrogate", ls, c2)

      i += 1
    }

    val end = nextChar()
    if (end != -1) extraChar(end)
  }

  private def expectedChar(prefix: String, ec: Int, fc: Int): Unit = {
    val pfx = if (prefix != null) s"$prefix " else ""
    val foundStr = if (fc != -1) f"0x$fc%x" else "EOF"
    throw new IOException(f"expected ${pfx}0x$ec%x but found $foundStr")
  }

  private def extraChar(c: Int): Unit = {
    throw new IOException(f"found extra character 0x$c%x")
  }

  /**
   * Produces a stream of UTF-16 byte sequences for all valid Unicode characters.
   */
  class UTF16Producer(val isBigEndian: Boolean) extends InputStream {
    private var fCodePoint: Int = 0
    private var fByte: Int = 0

    override def read(): Int = {
      if (fCodePoint < 0xFFFE) {
        if (fCodePoint == 0xD800) {
          fCodePoint = 0xE000
        }
        fByte match {
          case 0 =>
            val b = if (isBigEndian) fCodePoint >> 8 else fCodePoint & 0xFF
            fByte += 1
            b
          case 1 =>
            val b = if (isBigEndian) fCodePoint & 0xFF else fCodePoint >> 8
            fCodePoint += 1
            fByte = 0
            b
          case _ =>
            throw new RuntimeException(s"byte $fByte of 2-byte UTF-16 sequence")
        }
      } else {
        if (fCodePoint == 0xFFFE) {
          fCodePoint = 0x10000
        }
        if (fCodePoint < 0x110000) {
          val hs = XMLChar.highSurrogate(fCodePoint).toInt
          val ls = XMLChar.lowSurrogate(fCodePoint).toInt
          fByte match {
            case 0 =>
              val b = if (isBigEndian) hs >> 8 else hs & 0xFF
              fByte += 1
              b
            case 1 =>
              val b = if (isBigEndian) hs & 0xFF else hs >> 8
              fByte += 1
              b
            case 2 =>
              val b = if (isBigEndian) ls >> 8 else ls & 0xFF
              fByte += 1
              b
            case 3 =>
              val b = if (isBigEndian) ls & 0xFF else ls >> 8
              fCodePoint += 1
              fByte = 0
              b
            case _ =>
              throw new RuntimeException(s"byte $fByte of 4-byte UTF-16 sequence")
          }
        } else {
          -1
        }
      }
    }
  }
}
