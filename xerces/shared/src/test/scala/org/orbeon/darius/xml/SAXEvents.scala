package org.orbeon.darius.xml

import org.xml.sax.ext.LexicalHandler
import org.xml.sax.{Attributes, ContentHandler, Locator}

import scala.collection.mutable.ListBuffer


object SAXEvents {

  case class QName(namespaceURI: String, localPart: String, prefix: String)

  sealed trait SAXEvent

  case class  DocumentLocator    (locator: Locator)             extends SAXEvent
  case object StartDocument                                     extends SAXEvent
  case object EndDocument                                       extends SAXEvent
  case class  StartElement       (qName: QName, atts: Atts)     extends SAXEvent
  case class  EndElement         (qName: QName)                 extends SAXEvent
  case class  Characters         (text: String)                 extends SAXEvent
  case class  Comment            (text: String)                 extends SAXEvent
  case class  PI                 (target: String, data: String) extends SAXEvent
  case class  StartPrefixMapping (prefix: String, uri: String)  extends SAXEvent
  case class  EndPrefixMapping   (prefix: String)               extends SAXEvent

  case class Atts(atts: List[(QName, String)]) extends Attributes {
    def getLength: Int = atts.size

    def getURI      (index: Int): String = if (inRange(index)) atts(index)._1.namespaceURI                       else null
    def getLocalName(index: Int): String = if (inRange(index)) atts(index)._1.localPart                          else null
    def getQName    (index: Int): String = if (inRange(index)) buildQName(getPrefix(index), getLocalName(index)) else null
    def getType     (index: Int): String = if (inRange(index)) "CDATA"                                           else null
    def getValue    (index: Int): String = if (inRange(index)) atts(index)._2                                    else null

    def getIndex(uri: String, localName: String): Int =
      atts indexWhere { case (qName, _) => qName.namespaceURI == uri && qName.localPart == localName }

    def getIndex(qName: String): Int = {
      val (prefix, localName) = parseQName(qName)
      atts indexWhere { case (qName, _) => qName.prefix == prefix && qName.localPart == localName }
    }

    def getValue(uri: String, localName: String): String = getValue(getIndex(uri, localName))
    def getValue(qName: String): String = getValue(getIndex(qName))

    def getType(uri: String, localName: String): String = getType(getIndex(uri, localName))
    def getType(qName: String): String = getType(getIndex(qName))

    private def getPrefix(index: Int) = if (inRange(index)) atts(index)._1.prefix else null
    private def inRange(index: Int) = index >= 0 && index < atts.size
  }

  // Allow creating a StartElement with SAX-compatible parameters
  object StartElement {
    def apply(uri: String, localName: String, qName: String, atts: Attributes): StartElement =
      StartElement(new QName(uri, localName, prefixFromQName(qName)), Atts(atts))
  }

  // Allow creating an EndElement with SAX-compatible parameters
  object EndElement {
    def apply(uri: String, localName: String, qName: String): EndElement =
      EndElement(new QName(uri, localName, prefixFromQName(qName)))
  }

  object Atts {
    def apply(atts: Attributes): Atts = Atts(toSeq(atts))

    def toSeq(atts: Attributes): List[(QName, String)] = {
      val length = atts.getLength
      val result = ListBuffer[(QName, String)]()
      var i = 0
      while (i < length) {
        result += new QName(atts.getURI(i), atts.getLocalName(i), prefixFromQName(atts.getQName(i))) -> atts.getValue(i)
        i += 1
      }
      result.toList
    }

    implicit def tuplesToAtts1(atts: List[(QName, String)]): Atts = Atts(atts map { case (qName, value) => qName -> value })
    implicit def tuplesToAtts2(atts: List[(String, String)]): Atts = Atts(atts map { case (name, value) => new QName("", name, name) -> value })
  }

  object Characters {
    def apply(ch: Array[Char], start: Int, length: Int): Characters = Characters(new String(ch, start, length))
  }

  object Comment {
    def apply(ch: Array[Char], start: Int, length: Int): Characters = Characters(new String(ch, start, length))
  }

  private def buildQName(prefix: String, localname: String): String =
    if (prefix == null || prefix == "")
      localname
    else
      prefix + ":" + localname

  private def parseQName(lexicalQName: String): (String, String) =
    (prefixFromQName(lexicalQName), localNameFromQName(lexicalQName))

  private def prefixFromQName(qName: String): String = {
    val colonIndex = qName.indexOf(':')
    if (colonIndex == -1)
      ""
    else
      qName.substring(0, colonIndex)
  }

  private def localNameFromQName(qName: String): String = {
    val colonIndex = qName.indexOf(':')
    if (colonIndex == -(1))
      qName
    else
      qName.substring(colonIndex + 1)
  }
}

class AllCollector extends ContentHandler with LexicalHandler {

  import SAXEvents._

  private var _events = ListBuffer[SAXEvent]()
  def events: List[SAXEvent] = _events.result()

  def setDocumentLocator(locator: Locator)                                          : Unit = _events += DocumentLocator(locator)
  def startDocument()                                                               : Unit = _events += StartDocument
  def endDocument()                                                                 : Unit = _events += EndDocument
  def startPrefixMapping(prefix: String, uri: String)                               : Unit = _events += StartPrefixMapping(prefix, uri)
  def endPrefixMapping(prefix: String)                                              : Unit = _events += EndPrefixMapping(prefix)
  def startElement(uri: String, localName: String, qName: String, atts: Attributes) : Unit = _events += StartElement(uri, localName, qName, atts)
  def endElement(uri: String, localName: String, qName: String)                     : Unit = _events += EndElement(uri, localName, qName)
  def characters(ch: Array[Char], start: Int, length: Int)                          : Unit = _events += Characters(ch, start, length)
  def comment(ch: Array[Char], start: Int, length: Int)                             : Unit = _events += Comment(ch, start, length)
  def processingInstruction(target: String, data: String)                           : Unit = _events += PI(target, data)

  // Ignored events
  def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = ()
  def skippedEntity(name: String): Unit = ()
  def startDTD(name: String, publicId: String, systemId: String): Unit = ()
  def endDTD(): Unit = ()
  def startEntity(name: String): Unit = ()
  def endEntity(name: String): Unit = ()
  def startCDATA(): Unit = ()
  def endCDATA(): Unit = ()
}
