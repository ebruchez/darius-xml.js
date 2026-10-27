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
import org.orbeon.apache.xerces.impl.Constants
import org.orbeon.apache.xerces.parsers.{NonValidatingConfiguration, SAXParser}
import org.orbeon.apache.xerces.util.{SecurityManager, SymbolTable}
import org.orbeon.apache.xerces.xni.parser.{XMLInputSource, XMLParseException}
import org.xml.sax.{ErrorHandler, InputSource, SAXException, SAXParseException}
import org.xml.sax.helpers.DefaultHandler
import utest._

/**
 * Converted from Xerces Java test suite: `tests/jaxp/JAXPSecureProcessingTest.java`
 * and `tests/jaxp/data/gEntitySP.xml`, `pEntitySP.xml`, `pEntitySP.dtd`.
 *
 * Tests entity expansion limits (general and parameter entities) to prevent
 * XML entity expansion denial-of-service attacks (e.g. Billion Laughs).
 */
object SecurityProcessingTest extends TestSuite {

  private val SecurityManagerProperty =
    Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY

  // Billion laughs attack with general entities (gEntitySP.xml)
  private val GeneralEntityAttackXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<!DOCTYPE root [
      | <!ENTITY x0 "x">
      | <!ENTITY x1 "&x0;&x0;">
      | <!ENTITY x2 "&x1;&x1;">
      | <!ENTITY x3 "&x2;&x2;">
      | <!ENTITY x4 "&x3;&x3;">
      | <!ENTITY x5 "&x4;&x4;">
      | <!ENTITY x6 "&x5;&x5;">
      | <!ENTITY x7 "&x6;&x6;">
      | <!ENTITY x8 "&x7;&x7;">
      | <!ENTITY x9 "&x8;&x8;">
      | <!ENTITY x10 "&x9;&x9;">
      | <!ENTITY x11 "&x10;&x10;">
      | <!ENTITY x12 "&x11;&x11;">
      | <!ENTITY x13 "&x12;&x12;">
      | <!ENTITY x14 "&x13;&x13;">
      | <!ENTITY x15 "&x14;&x14;">
      | <!ENTITY x16 "&x15;&x15;">
      | <!ENTITY x17 "&x16;&x16;">
      | <!ENTITY x18 "&x17;&x17;">
      | <!ENTITY x19 "&x18;&x18;">
      | <!ENTITY x20 "&x19;&x19;">
      |]>
      |<root>&x20;</root>
      |""".stripMargin

  // Parameter entity expansion attack (pEntitySP.dtd)
  private val ParameterEntityAttackXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<!DOCTYPE root [
      | <!ENTITY % x0 "x">
      | <!ENTITY % x1 "%x0;%x0;">
      | <!ENTITY % x2 "%x1;%x1;">
      | <!ENTITY % x3 "%x2;%x2;">
      | <!ENTITY % x4 "%x3;%x3;">
      | <!ENTITY % x5 "%x4;%x4;">
      | <!ENTITY % x6 "%x5;%x5;">
      | <!ENTITY % x7 "%x6;%x6;">
      | <!ENTITY % x8 "%x7;%x7;">
      | <!ENTITY % x9 "%x8;%x8;">
      | <!ENTITY % x10 "%x9;%x9;">
      | <!ENTITY % x11 "%x10;%x10;">
      | <!ENTITY % x12 "%x11;%x11;">
      | <!ENTITY % x13 "%x12;%x12;">
      | <!ENTITY % x14 "%x13;%x13;">
      | <!ENTITY % x15 "%x14;%x14;">
      | <!ENTITY % x16 "%x15;%x15;">
      | <!ENTITY % x17 "%x16;%x16;">
      | <!ENTITY % x18 "%x17;%x17;">
      | <!ENTITY % x19 "%x18;%x18;">
      | <!ENTITY % x20 "%x19;%x19;">
      | %x20;
      |]>
      |<root/>
      |""".stripMargin

  private def createParser(limit: Int): SAXParser = {
    val secManager = new SecurityManager()
    secManager.setEntityExpansionLimit(limit)
    val config = new NonValidatingConfiguration(new SymbolTable)
    config.setProperty(SecurityManagerProperty, secManager)
    val parser = new SAXParser(config)
    parser
  }

  private def parseXml(parser: SAXParser, xml: String): Unit = {
    val in = new ByteArrayInputStream(xml.getBytes("UTF-8"))
    val source = new InputSource(in)
    source.setSystemId("test.xml")
    parser.parse(source)
  }

  override def tests = Tests {

    test("General entity expansion limit stops Billion Laughs attack") {
      val parser = createParser(limit = 100)
      var fatalErrorMessage: String = null

      parser.setErrorHandler(new ErrorHandler {
        def warning(e: SAXParseException): Unit = ()
        def error(e: SAXParseException): Unit = ()
        def fatalError(e: SAXParseException): Unit = {
          fatalErrorMessage = e.getMessage
          throw e
        }
      })

      val ex = intercept[SAXParseException] {
        parseXml(parser, GeneralEntityAttackXml)
      }
      assert(fatalErrorMessage != null)
      assert(fatalErrorMessage.contains("100") || fatalErrorMessage.toLowerCase.contains("entity"))
    }

    test("General entity expansion succeeds when below limit") {
      // Moderate entity expansion
      val moderateXml =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<!DOCTYPE root [
          | <!ENTITY a "hello">
          | <!ENTITY b "&a; &a;">
          | <!ENTITY c "&b; &b;">
          |]>
          |<root>&c;</root>
          |""".stripMargin

      val parser = createParser(limit = 100)
      var parsedText = ""
      parser.setContentHandler(new DefaultHandler {
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          parsedText += new String(ch, start, length)
        }
      })
      parseXml(parser, moderateXml)
      assert(parsedText == "hello hello hello hello")
    }

    test("Parameter entity expansion limit stops recursive parameter entities") {
      val parser = createParser(limit = 50)
      var fatalErrorMessage: String = null

      parser.setErrorHandler(new ErrorHandler {
        def warning(e: SAXParseException): Unit = ()
        def error(e: SAXParseException): Unit = ()
        def fatalError(e: SAXParseException): Unit = {
          fatalErrorMessage = e.getMessage
          throw e
        }
      })

      val ex = intercept[SAXParseException] {
        parseXml(parser, ParameterEntityAttackXml)
      }
      assert(fatalErrorMessage != null)
      assert(fatalErrorMessage.contains("50") || fatalErrorMessage.toLowerCase.contains("entity"))
    }

    test("Circular entity reference is detected as error") {
      val circularXml =
        """<?xml version="1.0"?>
          |<!DOCTYPE root [
          |  <!ENTITY a "&b;">
          |  <!ENTITY b "&a;">
          |]>
          |<root>&a;</root>
          |""".stripMargin

      val parser = createParser(limit = 1000)
      var reportedFatal = false
      parser.setErrorHandler(new ErrorHandler {
        def warning(e: SAXParseException): Unit = ()
        def error(e: SAXParseException): Unit = ()
        def fatalError(e: SAXParseException): Unit = {
          reportedFatal = true
          throw e
        }
      })

      intercept[SAXParseException] {
        parseXml(parser, circularXml)
      }
      assert(reportedFatal)
    }
  }
}
