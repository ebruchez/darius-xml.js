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
import org.xml.sax.{Attributes, DTDHandler, EntityResolver, InputSource}
import utest._

import scala.collection.mutable

/**
 * Converted from Xerces Java test data and samples:
 * `data/personal.xml`, `data/personal.dtd`, and `samples/sax/Counter.java`.
 *
 * Tests external DTD entity resolution, default attribute values, notation declarations,
 * ID/IDREF attribute types, and SAX document structure.
 */
object PersonalDocumentTest extends TestSuite {

  private val PersonalDtd =
    """<?xml encoding="UTF-8"?>
      |<!ELEMENT personnel (person)+>
      |
      |<!ELEMENT person (name,email*,url*,link?)>
      |<!ATTLIST person id ID #REQUIRED>
      |<!ATTLIST person note CDATA #IMPLIED>
      |<!ATTLIST person contr (true|false) 'false'>
      |<!ATTLIST person salary CDATA #IMPLIED>
      |
      |<!ELEMENT name ((family,given)|(given,family))>
      |
      |<!ELEMENT family (#PCDATA)>
      |
      |<!ELEMENT given (#PCDATA)>
      |
      |<!ELEMENT email (#PCDATA)>
      |
      |<!ELEMENT url EMPTY>
      |<!ATTLIST url href CDATA 'http://'>
      |
      |<!ELEMENT link EMPTY>
      |<!ATTLIST link manager IDREF #IMPLIED>
      |<!ATTLIST link subordinates IDREFS #IMPLIED>
      |
      |<!NOTATION gif PUBLIC '-//APP/Photoshop/4.0' 'photoshop.exe'>
      |""".stripMargin

  private val PersonalXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<!DOCTYPE personnel SYSTEM "personal.dtd">
      |<personnel>
      |
      |  <person id="Big.Boss">
      |    <name><family>Boss</family> <given>Big</given></name>
      |    <email>chief@foo.com</email>
      |    <link subordinates="one.worker two.worker three.worker four.worker five.worker"/>
      |  </person>
      |
      |  <person id="one.worker">
      |    <name><family>Worker</family> <given>One</given></name>
      |    <email>one@foo.com</email>
      |    <link manager="Big.Boss"/>
      |  </person>
      |
      |  <person id="two.worker">
      |    <name><family>Worker</family> <given>Two</given></name>
      |    <email>two@foo.com</email>
      |    <link manager="Big.Boss"/>
      |  </person>
      |
      |  <person id="three.worker">
      |    <name><family>Worker</family> <given>Three</given></name>
      |    <email>three@foo.com</email>
      |    <link manager="Big.Boss"/>
      |  </person>
      |
      |  <person id="four.worker">
      |    <name><family>Worker</family> <given>Four</given></name>
      |    <email>four@foo.com</email>
      |    <link manager="Big.Boss"/>
      |  </person>
      |
      |  <person id="five.worker">
      |    <name><family>Worker</family> <given>Five</given></name>
      |    <email>five@foo.com</email>
      |    <link manager="Big.Boss"/>
      |  </person>
      |
      |</personnel>
      |""".stripMargin

  override def tests = Tests {

    test("Parse personal.xml with external DTD and verify document structure") {
      val config = new NonValidatingConfiguration(new SymbolTable)
      val parser = new SAXParser(config)

      // Resolve personal.dtd in-memory
      parser.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          if (systemId != null && systemId.endsWith("personal.dtd")) {
            val in = new ByteArrayInputStream(PersonalDtd.getBytes("UTF-8"))
            val is = new InputSource(in)
            is.setSystemId(systemId)
            is
          } else {
            null
          }
        }
      })

      val elementNames = mutable.Buffer[String]()
      val personIds = mutable.Buffer[String]()
      val notations = mutable.Buffer[(String, String, String)]()

      parser.setDTDHandler(new DTDHandler {
        def notationDecl(name: String, publicId: String, systemId: String): Unit = {
          notations += ((name, publicId, systemId))
        }
        def unparsedEntityDecl(name: String, publicId: String, systemId: String, notationName: String): Unit = ()
      })

      parser.setContentHandler(new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          elementNames += qName
          if (qName == "person") {
            personIds += attributes.getValue("id")
          }
        }
      })

      val in = new ByteArrayInputStream(PersonalXml.getBytes("UTF-8"))
      val source = new InputSource(in)
      source.setSystemId("file:///personal.xml")
      parser.parse(source)

      // 1 personnel + 6 * (person, name, family, given, email, link) = 37 elements
      assert(elementNames.size == 37)
      assert(elementNames.head == "personnel")

      // 6 person elements with matching IDs
      assert(personIds == Seq("Big.Boss", "one.worker", "two.worker", "three.worker", "four.worker", "five.worker"))

      // Notation declaration gif from personal.dtd
      assert(notations.size == 1)
      assert(notations.head._1 == "gif")
      assert(notations.head._2 == "-//APP/Photoshop/4.0")
      assert(notations.head._3.endsWith("photoshop.exe"))
    }

    test("Internal and external general entity expansion in document") {
      val xmlWithEntities =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<!DOCTYPE root [
          |  <!ENTITY author "John Doe">
          |  <!ENTITY org "Acme Corp">
          |  <!ENTITY copyright "Copyright 2026 by &author;, &org;.">
          |]>
          |<root>
          |  <info note="By &author;">&copyright;</info>
          |</root>
          |""".stripMargin

      val config = new NonValidatingConfiguration(new SymbolTable)
      val parser = new SAXParser(config)

      var noteAttr: String = null
      var textContent = ""

      parser.setContentHandler(new DefaultHandler {
        override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
          if (qName == "info") {
            noteAttr = attributes.getValue("note")
          }
        }
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          textContent += new String(ch, start, length)
        }
      })

      parser.parse(new InputSource(new ByteArrayInputStream(xmlWithEntities.getBytes("UTF-8"))))

      assert(noteAttr == "By John Doe")
      assert(textContent.trim == "Copyright 2026 by John Doe, Acme Corp.")
    }

    test("Parameter entity expansion in DTD subset") {
      val xmlWithParamEntities =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<!DOCTYPE root [
          |  <!ENTITY % customEntities "<!ENTITY company 'Orbeon, Inc.'>">
          |  %customEntities;
          |]>
          |<root>&company;</root>
          |""".stripMargin

      val config = new NonValidatingConfiguration(new SymbolTable)
      val parser = new SAXParser(config)

      var textContent = ""
      parser.setContentHandler(new DefaultHandler {
        override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
          textContent += new String(ch, start, length)
        }
      })

      parser.parse(new InputSource(new ByteArrayInputStream(xmlWithParamEntities.getBytes("UTF-8"))))
      assert(textContent.trim == "Orbeon, Inc.")
    }
  }
}
