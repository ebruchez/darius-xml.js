/**
 * Copyright 2015 Orbeon, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.apache.xerces.demo

import org.orbeon.apache.xerces.xni._
import org.orbeon.apache.xerces.xni.parser.XMLDocumentSource

import scala.collection.compat._
import scala.collection.mutable.ListBuffer

sealed trait XMLEvent
case object StartDocument                                             extends XMLEvent
case object EndDocument                                               extends XMLEvent
case class  StartElement(qName: String, atts: List[(String, String)]) extends XMLEvent
case class  EndElement(qName: String)                                 extends XMLEvent
case class  Characters(text: String)                                  extends XMLEvent
case class  Comment(text: String)                                     extends XMLEvent
case class  PI(target: String, data: String)                          extends XMLEvent

class XMLEventCollector extends XMLDocumentHandler {

  private var namespaceContext: NamespaceContext = null

  private val _events = ListBuffer[XMLEvent]()
  def events = _events.toList

  def startDocument(
    locator          : XMLLocator,
    encoding         : String,
    namespaceContext : NamespaceContext,
    augs             : Augmentations
  ): Unit = {
    this.namespaceContext = namespaceContext
    _events += StartDocument
  }

  def endDocument(augs: Augmentations): Unit = {
    _events += EndDocument
  }

  private def collectAttributes(attributes: XMLAttributes) =
    for {
        i <- 0 until attributes.getLength
        qName = attributes.getQName(i)
        value = attributes.getValue(i)
      } yield qName -> value

  def startElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit =
    _events += StartElement(element.rawname, collectAttributes(attributes).to(List))

  def emptyElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit = {
    _events += StartElement(element.rawname, collectAttributes(attributes).to(List))
    _events += EndElement(element.rawname)
  }

  def endElement(element: QName, augs: Augmentations): Unit =
    _events += EndElement(element.rawname)

  def characters(text: XMLString, augs: Augmentations): Unit =
    _events += Characters(text.toString)

  def comment(text: XMLString, augs: Augmentations): Unit =
    _events += Comment(text.toString)

  def processingInstruction(target: String, data: XMLString, augs: Augmentations): Unit =
    _events += PI(target, data.toString)

  def xmlDecl(
    version    : String,
    encoding   : String,
    standalone : String,
    augs       : Augmentations
  ): Unit = ()

  def doctypeDecl(
    rootElement : String,
    publicId    : String,
    systemId    : String,
    augs        : Augmentations
  ): Unit = ()

  def startGeneralEntity(
    name: String,
    identifier: XMLResourceIdentifier,
    encoding: String,
    augs: Augmentations
  ): Unit = ()

  def textDecl(version: String, encoding: String, augs: Augmentations): Unit = ()
  def endGeneralEntity(name: String, augs: Augmentations): Unit = ()
  def ignorableWhitespace(text: XMLString, augs: Augmentations): Unit = ()

  def startCDATA(augs: Augmentations): Unit = ()
  def endCDATA(augs: Augmentations): Unit = ()

  def setDocumentSource(source: XMLDocumentSource): Unit = ()
  def getDocumentSource: XMLDocumentSource = null
}