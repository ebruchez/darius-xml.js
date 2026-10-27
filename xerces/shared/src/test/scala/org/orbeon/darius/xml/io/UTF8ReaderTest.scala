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
import org.orbeon.apache.xerces.impl.io.{MalformedByteSequenceException, UTF8Reader}
import utest._

/**
 * Converted from Xerces Java test suite: `tests/io/UTF8.java`.
 *
 * Tests the customized UTF-8 reader across the entire Unicode range (0x0 to 0x10FFFF)
 * both character-by-character and in block reads of varying sizes, as well as
 * malformed UTF-8 byte sequences.
 */
object UTF8ReaderTest extends TestSuite {

  private val BlockReadSize = 2048

  override def tests = Tests {

    test("UTF8Reader character by character over full Unicode range") {
      val stream = new UTF8Producer()
      val reader = new UTF8Reader(stream)
      try {
        testCharByChar(reader)
      } finally {
        reader.close()
      }
    }

    test("UTF8Reader block reads (2048 chars) over full Unicode range") {
      val stream = new UTF8Producer()
      val reader = new UTF8Reader(stream)
      try {
        testCharArray(reader, BlockReadSize)
      } finally {
        reader.close()
      }
    }

    test("UTF8Reader small block reads (17 chars)") {
      val stream = new UTF8Producer()
      val reader = new UTF8Reader(stream)
      try {
        testCharArray(reader, 17)
      } finally {
        reader.close()
      }
    }

    test("UTF8Reader rejects unexpected continuation byte") {
      // 0x80 is an unexpected continuation byte
      val in = new ByteArrayInputStream(Array(0x80.toByte))
      val reader = new UTF8Reader(in)
      val ex = intercept[MalformedByteSequenceException] {
        reader.read()
      }
      assert(ex.getMessage != null)
    }

    test("UTF8Reader rejects invalid lead bytes 0xFE and 0xFF") {
      val in1 = new ByteArrayInputStream(Array(0xFE.toByte))
      val reader1 = new UTF8Reader(in1)
      intercept[MalformedByteSequenceException] {
        reader1.read()
      }

      val in2 = new ByteArrayInputStream(Array(0xFF.toByte))
      val reader2 = new UTF8Reader(in2)
      intercept[MalformedByteSequenceException] {
        reader2.read()
      }
    }

    test("UTF8Reader rejects premature EOF in multi-byte sequence") {
      // 2-byte sequence start without continuation
      val in1 = new ByteArrayInputStream(Array(0xC2.toByte))
      val reader1 = new UTF8Reader(in1)
      intercept[MalformedByteSequenceException] {
        reader1.read()
      }

      // 3-byte sequence start with only 1 continuation byte
      val in2 = new ByteArrayInputStream(Array(0xE0.toByte, 0xA0.toByte))
      val reader2 = new UTF8Reader(in2)
      intercept[MalformedByteSequenceException] {
        reader2.read()
      }

      // 4-byte sequence start with only 2 continuation bytes
      val in3 = new ByteArrayInputStream(Array(0xF0.toByte, 0x90.toByte, 0x80.toByte))
      val reader3 = new UTF8Reader(in3)
      intercept[MalformedByteSequenceException] {
        reader3.read()
      }
    }

    test("UTF8Reader rejects overlong 2-byte sequence") {
      // 0xC0 0xAF is overlong encoding of ASCII '/' (0x2F)
      val in = new ByteArrayInputStream(Array(0xC0.toByte, 0xAF.toByte))
      val reader = new UTF8Reader(in)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }

    test("UTF8Reader rejects overlong 3-byte sequence") {
      // 0xE0 0x80 0xAF is overlong
      val in = new ByteArrayInputStream(Array(0xE0.toByte, 0x80.toByte, 0xAF.toByte))
      val reader = new UTF8Reader(in)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }

    test("UTF8Reader rejects overlong 4-byte sequence") {
      // 0xF0 0x80 0x80 0xAF is overlong
      val in = new ByteArrayInputStream(Array(0xF0.toByte, 0x80.toByte, 0x80.toByte, 0xAF.toByte))
      val reader = new UTF8Reader(in)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }

    test("UTF8Reader rejects surrogate halves in UTF-8") {
      // 0xED 0xA0 0x80 is 0xD800 (surrogate)
      val in = new ByteArrayInputStream(Array(0xED.toByte, 0xA0.toByte, 0x80.toByte))
      val reader = new UTF8Reader(in)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }

    test("UTF8Reader rejects code point greater than 0x10FFFF") {
      // 0xF4 0x90 0x80 0x80 is 0x110000 (> 0x10FFFF)
      val in = new ByteArrayInputStream(Array(0xF4.toByte, 0x90.toByte, 0x80.toByte, 0x80.toByte))
      val reader = new UTF8Reader(in)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }
    }
  }

  private def testCharByChar(reader: Reader): Unit = {
    // 0x000000 -> 0x00007F
    var i = 0
    while (i < 0x0080) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x000080 -> 0x0007FF
    while (i < 0x0800) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x000800 -> 0x00D7FF
    while (i < 0xD800) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x00E000 -> 0x00FFFF
    i = 0xE000
    while (i < 0x010000) {
      val c = reader.read()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x010000 -> 0x10FFFF
    while (i < 0x110000) {
      val uuuuu = (i >> 16) & 0x001F
      val wwww = uuuuu - 1
      val zzzz = (i >> 12) & 0x000F
      val yyyyyy = (i >> 6) & 0x003F
      val xxxxxx = i & 0x003F
      val hs = 0xD800 | (wwww << 6) | (zzzz << 2) | (yyyyyy >> 4)
      val ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx

      val c1 = reader.read()
      if (c1 != hs) expectedChar("high surrogate", hs, c1)

      val c2 = reader.read()
      if (c2 != ls) expectedChar("low surrogate", ls, c2)

      i += 1
    }

    // EOF
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

    // 0x000000 -> 0x00007F
    var i = 0
    while (i < 0x0080) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x000080 -> 0x0007FF
    while (i < 0x0800) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x000800 -> 0x00D7FF
    while (i < 0xD800) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x00E000 -> 0x00FFFF
    i = 0xE000
    while (i < 0x010000) {
      val c = nextChar()
      if (c != i) expectedChar(null, i, c)
      i += 1
    }

    // 0x010000 -> 0x10FFFF
    while (i < 0x110000) {
      val uuuuu = (i >> 16) & 0x001F
      val wwww = uuuuu - 1
      val zzzz = (i >> 12) & 0x000F
      val yyyyyy = (i >> 6) & 0x003F
      val xxxxxx = i & 0x003F
      val hs = 0xD800 | (wwww << 6) | (zzzz << 2) | (yyyyyy >> 4)
      val ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx

      val c1 = nextChar()
      if (c1 != hs) expectedChar("high surrogate", hs, c1)

      val c2 = nextChar()
      if (c2 != ls) expectedChar("low surrogate", ls, c2)

      i += 1
    }

    // EOF
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
   * Produces a stream of UTF-8 byte sequences for all valid Unicode characters.
   */
  class UTF8Producer extends InputStream {
    private var fCodePoint: Int = 0
    private var fByte: Int = 0

    override def read(): Int = {
      // 1-byte UTF-8: 0x000000 -> 0x00007F
      if (fCodePoint < 0x0080) {
        val b = fCodePoint
        fCodePoint += 1
        fByte = 0
        return b
      }

      // 2-byte UTF-8: 0x000080 -> 0x0007FF
      if (fCodePoint < 0x0800) {
        fByte match {
          case 0 =>
            val b = 0x00C0 | ((fCodePoint >> 6) & 0x001F)
            fByte += 1
            b
          case 1 =>
            val b = 0x0080 | (fCodePoint & 0x003F)
            fCodePoint += 1
            fByte = 0
            b
          case _ =>
            throw new RuntimeException(s"byte $fByte of 2-byte UTF-8 sequence")
        }
      } else if (fCodePoint < 0x10000) {
        // 3-byte UTF-8: 0x000800 -> 0x00FFFF (excluding surrogates 0xD800..0xDFFF)
        fByte match {
          case 0 =>
            val b = 0x00E0 | ((fCodePoint >> 12) & 0x000F)
            fByte += 1
            b
          case 1 =>
            val b = 0x0080 | ((fCodePoint >> 6) & 0x003F)
            fByte += 1
            b
          case 2 =>
            val b = 0x0080 | (fCodePoint & 0x003F)
            fCodePoint += 1
            if (fCodePoint == 0xD800) fCodePoint = 0xE000 // skip surrogate code points
            fByte = 0
            b
          case _ =>
            throw new RuntimeException(s"byte $fByte of 3-byte UTF-8 sequence")
        }
      } else if (fCodePoint < 0x110000) {
        // 4-byte UTF-8: 0x010000 -> 0x10FFFF
        fByte match {
          case 0 =>
            val uuuuu = (fCodePoint >> 16) & 0x001F
            val b = 0x00F0 | (uuuuu >> 2)
            fByte += 1
            b
          case 1 =>
            val uuuuu = (fCodePoint >> 16) & 0x001F
            val zzzz = (fCodePoint >> 12) & 0x000F
            val b = 0x0080 | ((uuuuu << 4) & 0x0030) | zzzz
            fByte += 1
            b
          case 2 =>
            val yyyyyy = (fCodePoint >> 6) & 0x003F
            val b = 0x0080 | yyyyyy
            fByte += 1
            b
          case 3 =>
            val xxxxxx = fCodePoint & 0x003F
            val b = 0x0080 | xxxxxx
            fCodePoint += 1
            fByte = 0
            b
          case _ =>
            throw new RuntimeException(s"byte $fByte of 4-byte UTF-8 sequence")
        }
      } else {
        -1
      }
    }
  }
}
