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

import java.io.{ByteArrayInputStream, IOException}
import org.orbeon.apache.xerces.impl.io.{ASCIIReader, Latin1Reader, MalformedByteSequenceException, UCSReader}
import org.orbeon.apache.xerces.util.XMLChar
import utest._

/**
 * Tests for ASCIIReader, Latin1Reader, and UCSReader.
 */
object OtherReadersTest extends TestSuite {

  override def tests = Tests {

    test("ASCIIReader character-by-character and block read") {
      val bytes = (0 until 128).map(_.toByte).toArray
      val in = new ByteArrayInputStream(bytes)
      val reader = new ASCIIReader(in, 128, new org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter)

      for (expected <- 0 until 128) {
        val c = reader.read()
        assert(c == expected)
      }
      assert(reader.read() == -1)

      // Test block read with buffer size >= 128
      val in2 = new ByteArrayInputStream(bytes)
      val reader2 = new ASCIIReader(in2, 128, new org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter)
      val ch = new Array[Char](128)
      val readCount = reader2.read(ch, 0, ch.length)
      assert(readCount == 128)
      for (i <- 0 until 128) {
        assert(ch(i).toInt == i)
      }
    }

    test("ASCIIReader rejects non-ASCII bytes (>= 0x80)") {
      val bytes = Array(0x41.toByte, 0x80.toByte, 0x42.toByte)
      val in = new ByteArrayInputStream(bytes)
      val reader = new ASCIIReader(in, 64, new org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter)
      assert(reader.read() == 'A'.toInt)
      intercept[MalformedByteSequenceException] {
        reader.read()
      }

      // In block read
      val in2 = new ByteArrayInputStream(bytes)
      val reader2 = new ASCIIReader(in2, 64, new org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter)
      val ch = new Array[Char](10)
      intercept[MalformedByteSequenceException] {
        reader2.read(ch, 0, ch.length)
      }
    }

    test("Latin1Reader character-by-character and block read") {
      val bytes = (0 until 256).map(_.toByte).toArray
      val in = new ByteArrayInputStream(bytes)
      val reader = new Latin1Reader(in, 256)

      for (expected <- 0 until 256) {
        val c = reader.read()
        assert(c == expected)
      }
      assert(reader.read() == -1)

      // Block read
      val in2 = new ByteArrayInputStream(bytes)
      val reader2 = new Latin1Reader(in2, 256)
      val ch = new Array[Char](256)
      val count = reader2.read(ch, 0, ch.length)
      assert(count == 256)
      for (i <- 0 until 256) {
        assert(ch(i).toInt == i)
      }
    }

    test("UCSReader UCS-4BE and UCS-4LE decoding") {
      // BMP: 'A' (0x00000041)
      // SMP: U+10400 (0x00010400)
      val beBytes = Array(
        0x00.toByte, 0x00.toByte, 0x00.toByte, 0x41.toByte, // 'A'
        0x00.toByte, 0x01.toByte, 0x04.toByte, 0x00.toByte  // U+10400
      )
      val readerBE = new UCSReader(new ByteArrayInputStream(beBytes), 64, UCSReader.UCS4BE)
      assert(readerBE.read() == 0x41)
      assert(readerBE.read() == 0x00010400)
      assert(readerBE.read() == -1)

      val leBytes = Array(
        0x41.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, // 'A'
        0x00.toByte, 0x04.toByte, 0x01.toByte, 0x00.toByte  // U+10400
      )
      val readerLE = new UCSReader(new ByteArrayInputStream(leBytes), 64, UCSReader.UCS4LE)
      assert(readerLE.read() == 0x41)
      assert(readerLE.read() == 0x00010400)
      assert(readerLE.read() == -1)
    }

    test("UCSReader UCS-2BE and UCS-2LE decoding") {
      val beBytes = Array(0x00.toByte, 0x42.toByte, 0x03.toByte, 0xC0.toByte) // 'B', greek pi
      val readerBE = new UCSReader(new ByteArrayInputStream(beBytes), 64, UCSReader.UCS2BE)
      assert(readerBE.read() == 0x42)
      assert(readerBE.read() == 0x03C0)
      assert(readerBE.read() == -1)

      val leBytes = Array(0x42.toByte, 0x00.toByte, 0xC0.toByte, 0x03.toByte)
      val readerLE = new UCSReader(new ByteArrayInputStream(leBytes), 64, UCSReader.UCS2LE)
      assert(readerLE.read() == 0x42)
      assert(readerLE.read() == 0x03C0)
      assert(readerLE.read() == -1)
    }

    test("UCSReader block read") {
      val beBytes = Array(
        0x00.toByte, 0x00.toByte, 0x00.toByte, 0x41.toByte,
        0x00.toByte, 0x00.toByte, 0x00.toByte, 0x42.toByte
      )
      val reader = new UCSReader(new ByteArrayInputStream(beBytes), 64, UCSReader.UCS4BE)
      val ch = new Array[Char](10)
      val count = reader.read(ch, 0, ch.length)
      assert(count == 2)
      assert(ch(0) == 'A')
      assert(ch(1) == 'B')
    }
  }
}
