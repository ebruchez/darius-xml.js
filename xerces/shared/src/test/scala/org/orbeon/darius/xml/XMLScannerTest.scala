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

package org.orbeon.darius.xml

import java.io.ByteArrayInputStream
import org.orbeon.apache.xerces.parsers.{NonValidatingConfiguration, SAXParser}
import org.orbeon.apache.xerces.util.SymbolTable
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{Attributes, InputSource, SAXParseException}
import utest._

import scala.collection.mutable

/**
 * Conformance tests for XML 1.0 scanner constructs:
 * - Processing Instructions (PIs)
 * - Comments
 * - CDATA sections
 * - Character references (decimal, hex, SMP)
 * - Predefined entities
 * - Attribute value normalization
 * - Namespace scoping and unbinding
 * - Well-formedness error detection
 */
object XMLScannerTest extends TestSuite {

  private def parse(xml: String, handler: DefaultHandler): Unit = {
    val config = new NonValidatingConfiguration(new SymbolTable)
    val parser = new SAXParser(config)
    parser.setContentHandler(handler)
    val in = new ByteArrayInputStream(xml.getBytes("UTF-8"))
    val source = new InputSource(in)
    source.setSystemId("test.xml")
    parser.parse(source)
  }

  private def parseExpectingFatalError(xml: String): SAXParseException = {
    val config = new NonValidatingConfiguration(new SymbolTable)
    val parser = new SAXParser(config)
    var captured: SAXParseException = null
    parser.setErrorHandler(new org.xml.sax.ErrorHandler {
      def warning(e: SAXParseException): Unit = ()
      def error(e: SAXParseException): Unit = ()
      def fatalError(e: SAXParseException): Unit = {
        captured = e
        throw e
      }
    })
    val in = new ByteArrayInputStream(xml.getBytes("UTF-8"))
    val source = new InputSource(in)
    source.setSystemId("test.xml")
    val thrown = intercept[SAXParseException] {
      parser.parse(source)
    }
    assert(captured != null)
    thrown
  }

  override def tests = Tests {

    test("Processing Instructions (PI)") {
      val xml =
        """<?xml version="1.0"?>
          |<?target1 data1?>
          |<root>
          |  <?target2 data with multiple words and symbols: = " '?>
          |  <?target3?>
          |</root>
          |<?target4 trailing data?>
          |""".stripMargin

      val pis = mutable.Buffer[(String, String)]()
      parse(xml, new DefaultHandler {
        override def processingInstruction(target: String, data: String): Unit = {
          pis += ((target, data))
        }
      })

      assert(pis.size == 4)
      assert(pis(0) == (("target1", "data1")))
      assert(pis(1) == (("target2", "data with multiple words and symbols: = \" '")))
      assert(pis(2) == (("target3", "")))
      assert(pis(3) == (("target4", "trailing data")))
    }

    test("Comments in misc and element content") {
      val xml =
        """<?xml version="1.0"?>
          |<!-- prolog comment -->
          |<root>
          |  <!-- comment with - single hyphen and newlines
          |       inside -->
          |  <child><!-- empty --></child>
          |</root>
          |<!-- epilog comment -->
          |""".stripMargin

      var rootVisited = false
      parse(xml, new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          if (qName == "root") rootVisited = true
        }
      })
      assert(rootVisited)
    }

    test("Rejects comment containing double-hyphen '--'") {
      val invalidXml =
        """<?xml version="1.0"?>
          |<root><!-- invalid -- comment --></root>
          |""".stripMargin

      parseExpectingFatalError(invalidXml)
    }

    test("CDATA sections including brackets and special characters") {
      val xml =
        """<?xml version="1.0"?>
          |<root>
          |  <![CDATA[This is <escaped> & "quoted" and contains ] and ]] but not closed yet]]>
          |  <![CDATA[]]>
          |</root>
          |""".stripMargin

      var textContent = ""
      parse(xml, new DefaultHandler {
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          textContent += new String(ch, start, length)
        }
      })

      assert(textContent.contains("""This is <escaped> & "quoted" and contains ] and ]] but not closed yet"""))
    }

    test("Character references (decimal, hex, and supplementary plane)") {
      // &#65; -> 'A'
      // &#x42; -> 'B'
      // &#x10400; -> U+10400 DESERET CAPITAL LETTER LONG I
      val xml =
        """<?xml version="1.0"?>
          |<root attr="&#65;&#x42;">&#65;&#x42;&#x10400;</root>
          |""".stripMargin

      var attrVal: String = null
      var textContent = ""

      parse(xml, new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          attrVal = attributes.getValue("attr")
        }
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          textContent += new String(ch, start, length)
        }
      })

      assert(attrVal == "AB")
      val expectedChars = "AB" + new String(Character.toChars(0x10400))
      assert(textContent == expectedChars)
    }

    test("Predefined entities in content and attributes") {
      val xml =
        """<?xml version="1.0"?>
          |<root attr="&lt;&gt;&amp;&quot;&apos;">&lt;&gt;&amp;&quot;&apos;</root>
          |""".stripMargin

      var attrVal: String = null
      var textContent = ""

      parse(xml, new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          attrVal = attributes.getValue("attr")
        }
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          textContent += new String(ch, start, length)
        }
      })

      val expected = """<>&"'"""
      assert(attrVal == expected)
      assert(textContent == expected)
    }

    test("Attribute value normalization: newlines and tabs normalized to spaces") {
      val xml =
        """<?xml version="1.0"?>
          |<root attr="line1
          |line2	tabbed"/>
          |""".stripMargin

      var attrVal: String = null
      parse(xml, new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          attrVal = attributes.getValue("attr")
        }
      })

      assert(attrVal == "line1 line2 tabbed")
    }

    test("Namespace scoping, default namespace, and unbinding") {
      val xml =
        """<?xml version="1.0"?>
          |<root xmlns="http://default.org" xmlns:p="http://prefix.org">
          |  <p:child>
          |    <inner xmlns="">
          |      <unbound/>
          |    </inner>
          |  </p:child>
          |</root>
          |""".stripMargin

      val elements = mutable.Buffer[(String, String, String)]() // (uri, localName, qName)
      val prefixEvents = mutable.Buffer[(String, String, Boolean)]() // (prefix, uri, isStart)

      parse(xml, new DefaultHandler {
        override def startPrefixMapping(prefix: String, uri: String): Unit = {
          prefixEvents += ((prefix, uri, true))
        }
        override def endPrefixMapping(prefix: String): Unit = {
          prefixEvents += ((prefix, "", false))
        }
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          elements += ((uri, localName, qName))
        }
      })

      // root: default ns http://default.org
      assert(elements(0) == (("http://default.org", "root", "root")))
      // p:child: ns http://prefix.org
      assert(elements(1) == (("http://prefix.org", "child", "p:child")))
      // inner: default ns reset to empty ""
      assert(elements(2) == (("", "inner", "inner")))
      // unbound: inherited empty ""
      assert(elements(3) == (("", "unbound", "unbound")))

      // Prefix events fired
      val startedPrefixes = prefixEvents.filter(_._3).map(p => (p._1, p._2))
      assert(startedPrefixes.contains(("", "http://default.org")))
      assert(startedPrefixes.contains(("p", "http://prefix.org")))
      assert(startedPrefixes.contains(("", ""))) // unbinding default ns
    }

    test("Well-formedness: rejects mismatched tags") {
      val xml = "<root><child></other></root>"
      parseExpectingFatalError(xml)
    }

    test("Well-formedness: rejects unclosed start tag") {
      val xml = "<root><unclosed>"
      parseExpectingFatalError(xml)
    }

    test("Well-formedness: rejects multiple root elements") {
      val xml = "<root1/><root2/>"
      parseExpectingFatalError(xml)
    }

    test("Well-formedness: rejects duplicate attributes") {
      val xml = """<root attr="1" attr="2"/>"""
      parseExpectingFatalError(xml)
    }

    test("Well-formedness: rejects invalid character in element name") {
      val xml = "<123root/>"
      parseExpectingFatalError(xml)
    }
  }
}
