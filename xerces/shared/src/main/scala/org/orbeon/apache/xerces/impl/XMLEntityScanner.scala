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

package org.orbeon.apache.xerces.impl

import java.io.EOFException
import java.io.IOException

import org.orbeon.apache.xerces.impl.XMLEntityScanner._
import org.orbeon.apache.xerces.impl.io.UCSReader
import org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter
import org.orbeon.apache.xerces.util.SymbolTable
import org.orbeon.apache.xerces.util.XMLChar
import org.orbeon.apache.xerces.util.XMLStringBuffer
import org.orbeon.apache.xerces.xni.QName
import org.orbeon.apache.xerces.xni.XMLLocator
import org.orbeon.apache.xerces.xni.XMLString


object XMLEntityScanner {

  private val DEBUG_ENCODINGS = false
  private val DEBUG_BUFFER = false

  /**
   * To signal the end of the document entity, this exception will be thrown.
   */
  private val END_OF_DOCUMENT_ENTITY = new EOFException() {
    override def fillInStackTrace(): Throwable = this
  }
}

/**
 * Implements the entity scanner methods.
 */
class XMLEntityScanner extends XMLLocator {

  private var fEntityManager: XMLEntityManager = null

  protected[impl] var fCurrentEntity: XMLEntityManager#ScannedEntity = null

  protected var fSymbolTable: SymbolTable = null

  protected var fBufferSize: Int = XMLEntityManager.DEFAULT_BUFFER_SIZE

  /**
   * Error reporter. This property identifier is:
   * http://apache.org/xml/properties/internal/error-reporter
   */
  protected var fErrorReporter: XMLErrorReporter = _

  /**
   * Returns the base system identifier of the currently scanned
   * entity, or null if none is available.
   */
  def getBaseSystemId: String = {
    if ((fCurrentEntity ne null) && (fCurrentEntity.entityLocation ne null))
      fCurrentEntity.entityLocation.getExpandedSystemId
    else
      null
  }

  /**
   * Sets the encoding of the scanner. This method is used by the
   * scanners if the XMLDecl or TextDecl line contains an encoding
   * pseudo-attribute.
   *
   * *Note:* The underlying character reader on the
   * current entity will be changed to accomodate the new encoding.
   * However, the new encoding is ignored if the current reader was
   * not constructed from an input stream (e.g. an external entity
   * that is resolved directly to the appropriate java.io.Reader
   * object).
   *
   * @param encoding The IANA encoding name of the new encoding.
   *
   * @throws IOException Thrown if the new encoding is not supported.
   */
  def setEncoding(encoding: String): Unit = {
    if (DEBUG_ENCODINGS) {
      println("$$$ setEncoding: " + encoding)
    }
    if (fCurrentEntity.stream ne null) {
      if ((fCurrentEntity.encoding eq null) || fCurrentEntity.encoding != encoding) {
        if ((fCurrentEntity.encoding ne null) && fCurrentEntity.encoding.startsWith("UTF-16")) {
          val ENCODING = encoding.toUpperCase//Locale.ENGLISH
          if (ENCODING == "UTF-16") return
          if (ENCODING == "ISO-10646-UCS-4") {
            fCurrentEntity.reader = if (fCurrentEntity.encoding == "UTF-16BE") new UCSReader(fCurrentEntity.stream,
              UCSReader.UCS4BE) else new UCSReader(fCurrentEntity.stream, UCSReader.UCS4LE)
            return
          }
          if (ENCODING == "ISO-10646-UCS-2") {
            fCurrentEntity.reader = if (fCurrentEntity.encoding == "UTF-16BE") new UCSReader(fCurrentEntity.stream,
              UCSReader.UCS2BE) else new UCSReader(fCurrentEntity.stream, UCSReader.UCS2LE)
            return
          }
        }
        if (DEBUG_ENCODINGS) {
          println("$$$ creating new reader from stream: " + fCurrentEntity.stream)
        }
        fCurrentEntity.setReader(fCurrentEntity.stream, encoding, null)
        fCurrentEntity.encoding = encoding
      } else {
        if (DEBUG_ENCODINGS) println("$$$ reusing old reader on stream")
      }
    }
  }

  /**
   * Sets the XML version. This method is used by the
   * scanners to report the value of the version pseudo-attribute
   * in an XML or text declaration.
   *
   * @param xmlVersion the XML version of the current entity
   */
  def setXMLVersion(xmlVersion: String): Unit = {
    fCurrentEntity.xmlVersion = xmlVersion
  }

  /**
   Returns true if the current entity being scanned is external.
   */
  def isExternal: Boolean = fCurrentEntity.isExternal

  /**
   * Returns the next character on the input.
   *
   * *Note:* The character is *not* consumed.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def peekChar(): Int = {
    if (DEBUG_BUFFER) {
      System.out.print("(peekChar: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    val c: Int = fCurrentEntity.ch(fCurrentEntity.position)
    if (DEBUG_BUFFER) {
      System.out.print(")peekChar: ")
      XMLEntityManager.print(fCurrentEntity)
      if (fCurrentEntity.isExternal) {
        println(" -> '" + (if (c != '\r') c.toChar else '\n') + "'")
      } else {
        println(" -> '" + c.toChar + "'")
      }
    }
    if (fCurrentEntity.isExternal) {
      if (c != '\r') c else '\n'
    } else {
      c
    }
  }

  /**
   * Returns the next character on the input.
   *
   * *Note:* The character is consumed.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanChar(): Int = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanChar: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var c: Int = fCurrentEntity.ch(fCurrentEntity.position)
    fCurrentEntity.position += 1
    if (c == '\n' || (c == '\r' && fCurrentEntity.isExternal)) {
      fCurrentEntity.lineNumber += 1
      fCurrentEntity.columnNumber = 1
      if (fCurrentEntity.position == fCurrentEntity.count) {
        fCurrentEntity.ch(0) = c.toChar
        load(1, changeEntity = false)
      }
      if (c == '\r' && fCurrentEntity.isExternal) {
        if (fCurrentEntity.ch(fCurrentEntity.position) == '\n') {
          fCurrentEntity.position += 1
        }
        c = '\n'
      }
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanChar: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> '" + c + "'")
    }
    fCurrentEntity.columnNumber += 1
    c
  }

  /**
   * Returns a string matching the NMTOKEN production appearing immediately
   * on the input as a symbol, or null if NMTOKEN Name string is present.
   *
   * *Note:* The NMTOKEN characters are consumed.
   *
   * *Note:* The string returned must be a symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanNmtoken(): String = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanNmtoken: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var offset = fCurrentEntity.position
    var exitLoop = false
    while (! exitLoop && XMLChar.isName(fCurrentEntity.ch(fCurrentEntity.position))) {
      fCurrentEntity.position += 1
      if (fCurrentEntity.position == fCurrentEntity.count) {
        val length = fCurrentEntity.position - offset
        if (length == fCurrentEntity.ch.length) {
          val tmp = new Array[Char](fCurrentEntity.ch.length << 1)
          System.arraycopy(fCurrentEntity.ch, offset, tmp, 0, length)
          fCurrentEntity.ch = tmp
        } else {
          System.arraycopy(fCurrentEntity.ch, offset, fCurrentEntity.ch, 0, length)
        }
        offset = 0
        if (load(length, changeEntity = false))
          exitLoop = true
      }
    }
    val length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length
    var symbol: String = null
    if (length > 0) {
      symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length)
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanNmtoken: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> " + String.valueOf(symbol))
    }
    symbol
  }

  /**
   * Returns a string matching the Name production appearing immediately
   * on the input as a symbol, or null if no Name string is present.
   *
   * *Note:* The Name characters are consumed.
   *
   * *Note:* The string returned must be a symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanName(): String = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanName: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var offset = fCurrentEntity.position
    if (XMLChar.isNameStart(fCurrentEntity.ch(offset))) {
      fCurrentEntity.position += 1
      if (fCurrentEntity.position == fCurrentEntity.count) {
        fCurrentEntity.ch(0) = fCurrentEntity.ch(offset)
        offset = 0
        if (load(1, changeEntity = false)) {
          fCurrentEntity.columnNumber += 1
          val symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1)
          if (DEBUG_BUFFER) {
            System.out.print(")scanName: ")
            XMLEntityManager.print(fCurrentEntity)
            println(" -> " + String.valueOf(symbol))
          }
          return symbol
        }
      }
      var exitLoop = false
      while (! exitLoop && XMLChar.isName(fCurrentEntity.ch(fCurrentEntity.position))) {
        fCurrentEntity.position += 1
        if (fCurrentEntity.position == fCurrentEntity.count) {
          val length = fCurrentEntity.position - offset
          if (length == fCurrentEntity.ch.length) {
            val tmp = new Array[Char](fCurrentEntity.ch.length << 1)
            System.arraycopy(fCurrentEntity.ch, offset, tmp, 0, length)
            fCurrentEntity.ch = tmp
          } else {
            System.arraycopy(fCurrentEntity.ch, offset, fCurrentEntity.ch, 0, length)
          }
          offset = 0
          if (load(length, changeEntity = false))
            exitLoop = true
        }
      }
    }
    val length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length
    var symbol: String = null
    if (length > 0) {
      symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length)
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanName: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> " + String.valueOf(symbol))
    }
    symbol
  }

  /**
   * Returns a string matching the NCName production appearing immediately
   * on the input as a symbol, or null if no NCName string is present.
   *
   * *Note:* The NCName characters are consumed.
   *
   * *Note:* The string returned must be a symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanNCName(): String = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanNCName: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var offset = fCurrentEntity.position
    if (XMLChar.isNCNameStart(fCurrentEntity.ch(offset))) {
      fCurrentEntity.position += 1
      if (fCurrentEntity.position == fCurrentEntity.count) {
        fCurrentEntity.ch(0) = fCurrentEntity.ch(offset)
        offset = 0
        if (load(1, changeEntity = false)) {
          fCurrentEntity.columnNumber += 1
          val symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1)
          if (DEBUG_BUFFER) {
            System.out.print(")scanNCName: ")
            XMLEntityManager.print(fCurrentEntity)
            println(" -> " + String.valueOf(symbol))
          }
          return symbol
        }
      }
      var exitLoop = false
      while (! exitLoop && XMLChar.isNCName(fCurrentEntity.ch(fCurrentEntity.position))) {
        fCurrentEntity.position += 1
        if (fCurrentEntity.position == fCurrentEntity.count) {
          val length = fCurrentEntity.position - offset
          if (length == fCurrentEntity.ch.length) {
            val tmp = new Array[Char](fCurrentEntity.ch.length << 1)
            System.arraycopy(fCurrentEntity.ch, offset, tmp, 0, length)
            fCurrentEntity.ch = tmp
          } else {
            System.arraycopy(fCurrentEntity.ch, offset, fCurrentEntity.ch, 0, length)
          }
          offset = 0
          if (load(length, changeEntity = false))
            exitLoop = true
        }
      }
    }
    val length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length
    var symbol: String = null
    if (length > 0) {
      symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length)
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanNCName: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> " + String.valueOf(symbol))
    }
    symbol
  }

  /**
   * Scans a qualified name from the input, setting the fields of the
   * QName structure appropriately.
   *
   * *Note:* The qualified name characters are consumed.
   *
   * *Note:* The strings used to set the values of the
   * QName structure must be symbols. The SymbolTable can be used for
   * this purpose.
   *
   * @param qname The qualified name structure to fill.
   *
   * @return Returns true if a qualified name appeared immediately on
   *         the input and was scanned, false otherwise.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanQName(qname: QName): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanQName, " + qname + ": ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var offset = fCurrentEntity.position
    if (XMLChar.isNCNameStart(fCurrentEntity.ch(offset))) {
      fCurrentEntity.position += 1
      if (fCurrentEntity.position == fCurrentEntity.count) {
        fCurrentEntity.ch(0) = fCurrentEntity.ch(offset)
        offset = 0
        if (load(1, changeEntity = false)) {
          fCurrentEntity.columnNumber += 1
          val name = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1)
          qname.setValues(null, name, name, null)
          if (DEBUG_BUFFER) {
            System.out.print(")scanQName, " + qname + ": ")
            XMLEntityManager.print(fCurrentEntity)
            println(" -> true")
          }
          return true
        }
      }
      var index = -1
      var exitLoop = false
      while (! exitLoop && XMLChar.isName(fCurrentEntity.ch(fCurrentEntity.position))) {
        val c = fCurrentEntity.ch(fCurrentEntity.position)
        if (c == ':') {
          if (index != -1)
            exitLoop = true
          else
            index = fCurrentEntity.position
        }
        if (! exitLoop) {
          fCurrentEntity.position += 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            val length = fCurrentEntity.position - offset
            if (length == fCurrentEntity.ch.length) {
              val tmp = new Array[Char](fCurrentEntity.ch.length << 1)
              System.arraycopy(fCurrentEntity.ch, offset, tmp, 0, length)
              fCurrentEntity.ch = tmp
            } else {
              System.arraycopy(fCurrentEntity.ch, offset, fCurrentEntity.ch, 0, length)
            }
            if (index != -1) {
              index = index - offset
            }
            offset = 0
            if (load(length, changeEntity = false))
              exitLoop = true
          }
        }
      }
      val length = fCurrentEntity.position - offset
      fCurrentEntity.columnNumber += length
      if (length > 0) {
        var prefix: String = null
        var localpart: String = null
        val rawname = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length)
        if (index != -1) {
          val prefixLength = index - offset
          prefix = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, prefixLength)
          val len = length - prefixLength - 1
          val startLocal = index + 1
          if (!XMLChar.isNCNameStart(fCurrentEntity.ch(startLocal))) {
            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "IllegalQName", null, XMLErrorReporter.SEVERITY_FATAL_ERROR)
          }
          localpart = fSymbolTable.addSymbol(fCurrentEntity.ch, startLocal, len)
        } else {
          localpart = rawname
        }
        qname.setValues(prefix, localpart, rawname, null)
        if (DEBUG_BUFFER) {
          System.out.print(")scanQName, " + qname + ": ")
          XMLEntityManager.print(fCurrentEntity)
          println(" -> true")
        }
        return true
      }
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanQName, " + qname + ": ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> false")
    }
    false
  }

  /**
   * Scans a range of parsed character data, setting the fields of the
   * XMLString structure, appropriately.
   *
   * *Note:* The characters are consumed.
   *
   * *Note:* This method does not guarantee to return
   * the longest run of parsed character data. This method may return
   * before markup due to reaching the end of the input buffer or any
   * other reason.
   *
   * *Note:* The fields contained in the XMLString
   * structure are not guaranteed to remain valid upon subsequent calls
   * to the entity scanner. Therefore, the caller is responsible for
   * immediately using the returned character data or making a copy of
   * the character data.
   *
   * @param content The content structure to fill.
   *
   * @return Returns the next character on the input, if known. This
   *         value may be -1 but this does *note* designate
   *         end of file.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanContent(content: XMLString): Int = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanContent: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    } else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
      fCurrentEntity.ch(0) = fCurrentEntity.ch(fCurrentEntity.count - 1)
      load(1, changeEntity = false)
      fCurrentEntity.position = 0
      fCurrentEntity.startPosition = 0
    }
    var offset = fCurrentEntity.position
    var c: Int = fCurrentEntity.ch(offset)
    var newlines = 0
    val external = fCurrentEntity.isExternal
    if (c == '\n' || (c == '\r' && external)) {
      if (DEBUG_BUFFER) {
        System.out.print("[newline, " + offset + ", " + fCurrentEntity.position + ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
      var exitLoop = false
      do {
        c = fCurrentEntity.ch(fCurrentEntity.position)
        fCurrentEntity.position += 1
        if (c == '\r' && external) {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
          if (! exitLoop) {
            if (fCurrentEntity.ch(fCurrentEntity.position) == '\n') {
              fCurrentEntity.position += 1
              offset += 1
            } else {
              newlines += 1
            }
          }
        } else if (c == '\n') {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
        } else {
          fCurrentEntity.position -= 1
          exitLoop = true
        }
      } while (! exitLoop && fCurrentEntity.position < fCurrentEntity.count - 1)
      for (i <- offset until fCurrentEntity.position)
        fCurrentEntity.ch(i) = '\n'
      val length = fCurrentEntity.position - offset
      if (fCurrentEntity.position == fCurrentEntity.count - 1) {
        content.setValues(fCurrentEntity.ch, offset, length)
        if (DEBUG_BUFFER) {
          System.out.print("]newline, " + offset + ", " + fCurrentEntity.position + ": ")
          XMLEntityManager.print(fCurrentEntity)
          println()
        }
        return -1
      }
      if (DEBUG_BUFFER) {
        System.out.print("]newline, " + offset + ", " + fCurrentEntity.position + ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
    }
    var exitLoop = false
    while (! exitLoop && fCurrentEntity.position < fCurrentEntity.count) {
      c = fCurrentEntity.ch(fCurrentEntity.position)
      fCurrentEntity.position += 1
      if (! XMLChar.isContent(c)) {
        fCurrentEntity.position -= 1
        exitLoop = true
      }
    }
    val length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length - newlines
    content.setValues(fCurrentEntity.ch, offset, length)
    if (fCurrentEntity.position != fCurrentEntity.count) {
      c = fCurrentEntity.ch(fCurrentEntity.position)
      if (c == '\r' && external) {
        c = '\n'
      }
    } else {
      c = -1
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanContent: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> '" + c + "'")
    }
    c
  }

  /**
   * Scans a range of attribute value data, setting the fields of the
   * XMLString structure, appropriately.
   *
   * *Note:* The characters are consumed.
   *
   * *Note:* This method does not guarantee to return
   * the longest run of attribute value data. This method may return
   * before the quote character due to reaching the end of the input
   * buffer or any other reason.
   *
   * *Note:* The fields contained in the XMLString
   * structure are not guaranteed to remain valid upon subsequent calls
   * to the entity scanner. Therefore, the caller is responsible for
   * immediately using the returned character data or making a copy of
   * the character data.
   *
   * @param quote   The quote character that signifies the end of the
   *                attribute value data.
   * @param content The content structure to fill.
   *
   * @return Returns the next character on the input, if known. This
   *         value may be -1 but this does *note* designate
   *         end of file.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanLiteral(quote: Int, content: XMLString): Int = {
    if (DEBUG_BUFFER) {
      System.out.print("(scanLiteral, '" + quote.toChar + "': ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    } else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
      fCurrentEntity.ch(0) = fCurrentEntity.ch(fCurrentEntity.count - 1)
      load(1, changeEntity = false)
      fCurrentEntity.position = 0
      fCurrentEntity.startPosition = 0
    }
    var offset = fCurrentEntity.position
    var c: Int = fCurrentEntity.ch(offset)
    var newlines = 0
    val external = fCurrentEntity.isExternal
    if (c == '\n' || (c == '\r' && external)) {
      if (DEBUG_BUFFER) {
        System.out.print("[newline, " + offset + ", " + fCurrentEntity.position +
          ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
      var exitLoop = false
      do {
        c = fCurrentEntity.ch(fCurrentEntity.position)
        fCurrentEntity.position += 1
        if (c == '\r' && external) {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
          if (! exitLoop) {
            if (fCurrentEntity.ch(fCurrentEntity.position) == '\n') {
              fCurrentEntity.position += 1
              offset += 1
            } else {
              newlines += 1
            }
          }
        } else if (c == '\n') {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
        } else {
          fCurrentEntity.position -= 1
          exitLoop = true
        }
      } while (! exitLoop && fCurrentEntity.position < fCurrentEntity.count - 1)
      for (i <- offset until fCurrentEntity.position)
        fCurrentEntity.ch(i) = '\n'
      val length = fCurrentEntity.position - offset
      if (fCurrentEntity.position == fCurrentEntity.count - 1) {
        content.setValues(fCurrentEntity.ch, offset, length)
        if (DEBUG_BUFFER) {
          System.out.print("]newline, " + offset + ", " + fCurrentEntity.position +
            ": ")
          XMLEntityManager.print(fCurrentEntity)
          println()
        }
        return -1
      }
      if (DEBUG_BUFFER) {
        System.out.print("]newline, " + offset + ", " + fCurrentEntity.position +
          ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
    }
    var exitLoop = false
    while (! exitLoop && fCurrentEntity.position < fCurrentEntity.count) {
      c = fCurrentEntity.ch(fCurrentEntity.position)
      fCurrentEntity.position += 1
      if ((c == quote && (!fCurrentEntity.literal || external)) || c == '%' || !XMLChar.isContent(c)) {
        fCurrentEntity.position -= 1
        exitLoop = true
      }
    }
    val length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length - newlines
    content.setValues(fCurrentEntity.ch, offset, length)
    if (fCurrentEntity.position != fCurrentEntity.count) {
      c = fCurrentEntity.ch(fCurrentEntity.position)
      if (c == quote && fCurrentEntity.literal) {
        c = -1
      }
    } else {
      c = -1
    }
    if (DEBUG_BUFFER) {
      System.out.print(")scanLiteral, '" + quote.toChar + "': ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> '" + c + "'")
    }
    c
  }

  /**
   * Scans a range of character data up to the specified delimiter,
   * setting the fields of the XMLString structure, appropriately.
   *
   * *Note:* The characters are consumed.
   *
   * *Note:* This assumes that the internal buffer is
   * at least the same size, or bigger, than the length of the delimiter
   * and that the delimiter contains at least one character.
   *
   * *Note:* This method does not guarantee to return
   * the longest run of character data. This method may return before
   * the delimiter due to reaching the end of the input buffer or any
   * other reason.
   *
   * *Note:* The fields contained in the XMLString
   * structure are not guaranteed to remain valid upon subsequent calls
   * to the entity scanner. Therefore, the caller is responsible for
   * immediately using the returned character data or making a copy of
   * the character data.
   *
   * @param delimiter The string that signifies the end of the character
   *                  data to be scanned.
   * @param buffer    The XMLStringBuffer to fill.
   *
   * @return Returns true if there is more data to scan, false otherwise.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def scanData(delimiter: String, buffer: XMLStringBuffer): Boolean = {
    var found = false
    val delimLen = delimiter.length
    val charAt0 = delimiter.charAt(0)
    val external = fCurrentEntity.isExternal
    if (DEBUG_BUFFER) {
      System.out.print("(scanData: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var bNextEntity = false
    while ((fCurrentEntity.position > fCurrentEntity.count - delimLen) &&
      (!bNextEntity)) {
      System.arraycopy(fCurrentEntity.ch, fCurrentEntity.position, fCurrentEntity.ch, 0, fCurrentEntity.count - fCurrentEntity.position)
      bNextEntity = load(fCurrentEntity.count - fCurrentEntity.position, changeEntity = false)
      fCurrentEntity.position = 0
      fCurrentEntity.startPosition = 0
    }
    if (fCurrentEntity.position > fCurrentEntity.count - delimLen) {
      val length = fCurrentEntity.count - fCurrentEntity.position
      buffer.append(fCurrentEntity.ch, fCurrentEntity.position, length)
      fCurrentEntity.columnNumber += fCurrentEntity.count
      fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
      fCurrentEntity.position = fCurrentEntity.count
      fCurrentEntity.startPosition = fCurrentEntity.count
      load(0, changeEntity = true)
      return false
    }
    var offset = fCurrentEntity.position
    var c: Int = fCurrentEntity.ch(offset)
    var newlines = 0
    if (c == '\n' || (c == '\r' && external)) {
      if (DEBUG_BUFFER) {
        System.out.print("[newline, " + offset + ", " + fCurrentEntity.position +
          ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
      var exitLoop = false
      do {
        c = fCurrentEntity.ch(fCurrentEntity.position)
        fCurrentEntity.position += 1
        if (c == '\r' && external) {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
          if (! exitLoop) {
            if (fCurrentEntity.ch(fCurrentEntity.position) == '\n') {
              fCurrentEntity.position += 1
              offset += 1
            } else {
              newlines += 1
            }
          }
        } else if (c == '\n') {
          newlines += 1
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count) {
            offset = 0
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
            fCurrentEntity.position = newlines
            fCurrentEntity.startPosition = newlines
            fCurrentEntity.count = newlines
            if (load(newlines, changeEntity = false))
              exitLoop = true
          }
        } else {
          fCurrentEntity.position -= 1
          exitLoop = true
        }
      } while (! exitLoop && fCurrentEntity.position < fCurrentEntity.count - 1)
      for (i <- offset until fCurrentEntity.position)
        fCurrentEntity.ch(i) = '\n'
      val length = fCurrentEntity.position - offset
      if (fCurrentEntity.position == fCurrentEntity.count - 1) {
        buffer.append(fCurrentEntity.ch, offset, length)
        if (DEBUG_BUFFER) {
          System.out.print("]newline, " + offset + ", " + fCurrentEntity.position +
            ": ")
          XMLEntityManager.print(fCurrentEntity)
          println()
        }
        return true
      }
      if (DEBUG_BUFFER) {
        System.out.print("]newline, " + offset + ", " + fCurrentEntity.position +
          ": ")
        XMLEntityManager.print(fCurrentEntity)
        println()
      }
    }

    locally {
      var exitOuterLoop = false
      while (! exitOuterLoop && fCurrentEntity.position < fCurrentEntity.count) {
        c = fCurrentEntity.ch(fCurrentEntity.position)
        fCurrentEntity.position += 1
        if (c == charAt0) {
          val delimOffset = fCurrentEntity.position - 1

          locally {
            var i = 0
            var exitInnerLoop = false
            while (! exitInnerLoop && i < delimLen) {
              if (fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.position -= i
                exitInnerLoop = true
                exitOuterLoop = true
              } else {
                c = fCurrentEntity.ch(fCurrentEntity.position)
                fCurrentEntity.position += 1
                if (delimiter.charAt(i) != c) {
                  fCurrentEntity.position -= 1
                  exitInnerLoop = true
                } else {
                  i += 1
                }
              }
            }
          }
          if (! exitOuterLoop && fCurrentEntity.position == delimOffset + delimLen) {
            found = true
            exitOuterLoop = true
          }
        } else if (c == '\n' || (external && c == '\r')) {
          fCurrentEntity.position -= 1
          exitOuterLoop = true
        } else if (XMLChar.isInvalid(c)) {
          fCurrentEntity.position -= 1
          val length = fCurrentEntity.position - offset
          fCurrentEntity.columnNumber += length - newlines
          buffer.append(fCurrentEntity.ch, offset, length)
          return true
        }
      }
    }
    var length = fCurrentEntity.position - offset
    fCurrentEntity.columnNumber += length - newlines
    if (found) {
      length -= delimLen
    }
    buffer.append(fCurrentEntity.ch, offset, length)
    if (DEBUG_BUFFER) {
      System.out.print(")scanData: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> " + !found)
    }
    !found
  }

  /**
   * Skips a character appearing immediately on the input.
   *
   * *Note:* The character is consumed only if it matches
   * the specified character.
   *
   * @param c The character to skip.
   *
   * @return Returns true if the character was skipped.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def skipChar(c: Int): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(skipChar, '" + c.toChar + "': ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    val cc = fCurrentEntity.ch(fCurrentEntity.position)
    if (cc == c) {
      fCurrentEntity.position += 1
      if (c == '\n') {
        fCurrentEntity.lineNumber += 1
        fCurrentEntity.columnNumber = 1
      } else {
        fCurrentEntity.columnNumber += 1
      }
      if (DEBUG_BUFFER) {
        System.out.print(")skipChar, '" + c.toChar + "': ")
        XMLEntityManager.print(fCurrentEntity)
        println(" -> true")
      }
      return true
    } else if (c == '\n' && cc == '\r' && fCurrentEntity.isExternal) {
      if (fCurrentEntity.position == fCurrentEntity.count) {
        fCurrentEntity.ch(0) = cc.toChar
        load(1, changeEntity = false)
      }
      fCurrentEntity.position += 1
      if (fCurrentEntity.ch(fCurrentEntity.position) == '\n') {
        fCurrentEntity.position += 1
      }
      fCurrentEntity.lineNumber += 1
      fCurrentEntity.columnNumber = 1
      if (DEBUG_BUFFER) {
        System.out.print(")skipChar, '" + c.toChar + "': ")
        XMLEntityManager.print(fCurrentEntity)
        println(" -> true")
      }
      return true
    }
    if (DEBUG_BUFFER) {
      System.out.print(")skipChar, '" + c.toChar + "': ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> false")
    }
    false
  }

  /**
   * Skips space characters appearing immediately on the input.
   *
   * *Note:* The characters are consumed only if they are
   * space characters.
   *
   * @return Returns true if at least one space character was skipped.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def skipSpaces(): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(skipSpaces: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var c: Int = fCurrentEntity.ch(fCurrentEntity.position)
    if (XMLChar.isSpace(c)) {
      val external = fCurrentEntity.isExternal
      do {
        var entityChanged = false
        if (c == '\n' || (external && c == '\r')) {
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            fCurrentEntity.ch(0) = c.toChar
            entityChanged = load(1, changeEntity = true)
            if (!entityChanged) {
              fCurrentEntity.position = 0
              fCurrentEntity.startPosition = 0
            }
          }
          if (c == '\r' && external) {
            fCurrentEntity.position += 1
            if (fCurrentEntity.ch(fCurrentEntity.position) != '\n') {
              fCurrentEntity.position -= 1
            }
          }
        } else {
          fCurrentEntity.columnNumber += 1
        }
        if (!entityChanged) fCurrentEntity.position += 1
        if (fCurrentEntity.position == fCurrentEntity.count) {
          load(0, changeEntity = true)
        }
      } while (XMLChar.isSpace({ c = fCurrentEntity.ch(fCurrentEntity.position); c }))
      if (DEBUG_BUFFER) {
        System.out.print(")skipSpaces: ")
        XMLEntityManager.print(fCurrentEntity)
        println(" -> true")
      }
      return true
    }
    if (DEBUG_BUFFER) {
      System.out.print(")skipSpaces: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> false")
    }
    false
  }

  /**
   * Skips space characters appearing immediately on the input that would
   * match non-terminal S (0x09, 0x0A, 0x0D, 0x20) before end of line
   * normalization is performed. This is useful when scanning structures
   * such as the XMLDecl and TextDecl that can only contain US-ASCII
   * characters.
   *
   * *Note:* The characters are consumed only if they would
   * match non-terminal S before end of line normalization is performed.
   *
   * @return Returns true if at least one space character was skipped.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def skipDeclSpaces(): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(skipDeclSpaces: ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count) {
      load(0, changeEntity = true)
    }
    var c: Int = fCurrentEntity.ch(fCurrentEntity.position)
    if (XMLChar.isSpace(c)) {
      val external = fCurrentEntity.isExternal
      do {
        var entityChanged = false
        if (c == '\n' || (external && c == '\r')) {
          fCurrentEntity.lineNumber += 1
          fCurrentEntity.columnNumber = 1
          if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            fCurrentEntity.ch(0) = c.toChar
            entityChanged = load(1, changeEntity = true)
            if (!entityChanged) {
              fCurrentEntity.position = 0
              fCurrentEntity.startPosition = 0
            }
          }
          if (c == '\r' && external) {
            fCurrentEntity.position += 1
            if (fCurrentEntity.ch(fCurrentEntity.position) != '\n') {
              fCurrentEntity.position -= 1
            }
          }
        } else {
          fCurrentEntity.columnNumber += 1
        }
        if (!entityChanged) fCurrentEntity.position += 1
        if (fCurrentEntity.position == fCurrentEntity.count) {
          load(0, changeEntity = true)
        }
      } while (XMLChar.isSpace({ c = fCurrentEntity.ch(fCurrentEntity.position); c }))
      if (DEBUG_BUFFER) {
        System.out.print(")skipDeclSpaces: ")
        XMLEntityManager.print(fCurrentEntity)
        println(" -> true")
      }
      return true
    }
    if (DEBUG_BUFFER) {
      System.out.print(")skipDeclSpaces: ")
      XMLEntityManager.print(fCurrentEntity)
      println(" -> false")
    }
    false
  }

  /**
   * Skips the specified string appearing immediately on the input.
   *
   * *Note:* The characters are consumed only if they are
   * space characters.
   *
   * @param s The string to skip.
   *
   * @return Returns true if the string was skipped.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws EOFException Thrown on end of file.
   */
  def skipString(s: String): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(skipString, \"" + s + "\": ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    if (fCurrentEntity.position == fCurrentEntity.count)
      load(0, changeEntity = true)
    // ORBEON: Avoid non-local return.
    val length = s.length
    var exitLoop = false
    var i = 0
    while (! exitLoop && i < length) {
      val c = fCurrentEntity.ch(fCurrentEntity.position)
      fCurrentEntity.position += 1
      if (c != s.charAt(i)) {
        fCurrentEntity.position -= i + 1
        if (DEBUG_BUFFER) {
          System.out.print(")skipString, \"" + s + "\": ")
          XMLEntityManager.print(fCurrentEntity)
          println(" -> false")
        }
        exitLoop = true
      } else if (i < length - 1 && fCurrentEntity.position == fCurrentEntity.count) {
        System.arraycopy(fCurrentEntity.ch, fCurrentEntity.count - i - 1, fCurrentEntity.ch, 0, i + 1)
        if (load(i + 1, changeEntity = false)) {
          fCurrentEntity.startPosition -= i + 1
          fCurrentEntity.position -= i + 1
          if (DEBUG_BUFFER) {
            System.out.print(")skipString, \"" + s + "\": ")
            XMLEntityManager.print(fCurrentEntity)
            println(" -> false")
          }
          exitLoop = true
        } else {
          i += 1
        }
      } else {
        i += 1
      }
    }
    if (exitLoop) {
      false
    } else {
      if (DEBUG_BUFFER) {
        System.out.print(")skipString, \"" + s + "\": ")
        XMLEntityManager.print(fCurrentEntity)
        println(" -> true")
      }
      fCurrentEntity.columnNumber += length
      true
    }
  }

  /**
   * Return the public identifier for the current document event.
   *
   * The return value is the public identifier of the document
   * entity or of the external parsed entity in which the markup
   * triggering the event appears.
   *
   * @return A string containing the public identifier, or
   *         null if none is available.
   */
  def getPublicId: String = {
    if ((fCurrentEntity ne null) && (fCurrentEntity.entityLocation ne null))
      fCurrentEntity.entityLocation.getPublicId
    else
      null
  }

  /**
   * Return the expanded system identifier for the current document event.
   *
   * The return value is the expanded system identifier of the document
   * entity or of the external parsed entity in which the markup
   * triggering the event appears.
   *
   * If the system identifier is a URL, the parser must resolve it
   * fully before passing it to the application.
   *
   * @return A string containing the expanded system identifier, or null
   *         if none is available.
   */
  def getExpandedSystemId: String = {
    if (fCurrentEntity ne null) {
      if ((fCurrentEntity.entityLocation ne null) && (fCurrentEntity.entityLocation.getExpandedSystemId ne null)) {
        return fCurrentEntity.entityLocation.getExpandedSystemId
      } else {
        return fCurrentEntity.getExpandedSystemId
      }
    }
    null
  }

  /**
   * Return the literal system identifier for the current document event.
   *
   * The return value is the literal system identifier of the document
   * entity or of the external parsed entity in which the markup
   * triggering the event appears.
   *
   * @return A string containing the literal system identifier, or null
   *         if none is available.
   */
  def getLiteralSystemId: String = {
    if (fCurrentEntity ne null) {
      if ((fCurrentEntity.entityLocation ne null) && (fCurrentEntity.entityLocation.getLiteralSystemId ne null)) {
        return fCurrentEntity.entityLocation.getLiteralSystemId
      } else {
        return fCurrentEntity.getLiteralSystemId
      }
    }
    null
  }

  /**
   * Returns the line number where the current document event ends.
   *
   * *Warning:* The return value from the method
   * is intended only as an approximation for the sake of error
   * reporting; it is not intended to provide sufficient information
   * to edit the character content of the original XML document.
   *
   * The return value is an approximation of the line number
   * in the document entity or external parsed entity where the
   * markup triggering the event appears.
   *
   * If possible, the line position of the first character after the
   * text associated with the document event should be provided.
   * The first line in the document is line 1.
   *
   * @return The line number, or -1 if none is available.
   */
  def getLineNumber: Int = {
    if (fCurrentEntity ne null) {
      if (fCurrentEntity.isExternal) {
        return fCurrentEntity.lineNumber
      } else {
        return fCurrentEntity.getLineNumber
      }
    }
    -1
  }

  /**
   * Returns the column number where the current document event ends.
   *
   * *Warning:* The return value from the method
   * is intended only as an approximation for the sake of error
   * reporting; it is not intended to provide sufficient information
   * to edit the character content of the original XML document.
   *
   * The return value is an approximation of the column number
   * in the document entity or external parsed entity where the
   * markup triggering the event appears.
   *
   * If possible, the line position of the first character after the
   * text associated with the document event should be provided.
   * The first column in each line is column 1.
   *
   * @return The column number, or -1 if none is available.
   */
  def getColumnNumber: Int = {
    if (fCurrentEntity ne null) {
      if (fCurrentEntity.isExternal) {
        return fCurrentEntity.columnNumber
      } else {
        return fCurrentEntity.getColumnNumber
      }
    }
    -1
  }

  /**
   * Returns the character offset where the current document event ends.
   *
   * *Warning:* The return value from the method
   * is intended only as an approximation for the sake of error
   * reporting; it is not intended to provide sufficient information
   * to edit the character content of the original XML document.
   *
   * The return value is an approximation of the character offset
   * in the document entity or external parsed entity where the
   * markup triggering the event appears.
   *
   * If possible, the character offset of the first character after the
   * text associated with the document event should be provided.
   *
   * @return The character offset, or -1 if none is available.
   */
  def getCharacterOffset: Int = {
    if (fCurrentEntity ne null) {
      if (fCurrentEntity.isExternal) {
        return fCurrentEntity.baseCharOffset +
          (fCurrentEntity.position - fCurrentEntity.startPosition)
      } else {
        return fCurrentEntity.getCharacterOffset
      }
    }
    -1
  }

  /**
   * Returns the encoding of the current entity.
   * Note that, for a given entity, this value can only be
   * considered final once the encoding declaration has been read (or once it
   * has been determined that there is no such declaration) since, no encoding
   * having been specified on the XMLInputSource, the parser
   * will make an initial "guess" which could be in error.
   */
  def getEncoding: String = {
    if (fCurrentEntity ne null) {
      if (fCurrentEntity.isExternal) {
        return fCurrentEntity.encoding
      } else {
        return fCurrentEntity.getEncoding
      }
    }
    null
  }

  /**
   * Returns the XML version of the current entity. This will normally be the
   * value from the XML or text declaration or defaulted by the parser. Note that
   * that this value may be different than the version of the processing rules
   * applied to the current entity. For instance, an XML 1.1 document may refer to
   * XML 1.0 entities. In such a case the rules of XML 1.1 are applied to the entire
   * document. Also note that, for a given entity, this value can only be considered
   * final once the XML or text declaration has been read or once it has been
   * determined that there is no such declaration.
   */
  def getXMLVersion: String = {
    if (fCurrentEntity ne null) {
      if (fCurrentEntity.isExternal) {
        return fCurrentEntity.xmlVersion
      } else {
        return fCurrentEntity.getXMLVersion
      }
    }
    null
  }

  def setCurrentEntity(ent: XMLEntityManager#ScannedEntity): Unit = {
    fCurrentEntity = ent
  }

  def setBufferSize(size: Int): Unit = {
    fBufferSize = size
  }

  def reset(symbolTable: SymbolTable, entityManager: XMLEntityManager, reporter: XMLErrorReporter): Unit = {
    fCurrentEntity = null
    fSymbolTable = symbolTable
    fEntityManager = entityManager
    fErrorReporter = reporter
  }

  /**
   * Loads a chunk of text.
   *
   * @param offset       The offset into the character buffer to
   *                     read the next batch of characters.
   * @param changeEntity True if the load should change entities
   *                     at the end of the entity, otherwise leave
   *                     the current entity in place and the entity
   *                     boundary will be signaled by the return
   *                     value.
   *
   * @return Returns true if the entity changed as a result of this
   *          load operation.
   */
  def load(offset: Int, changeEntity: Boolean): Boolean = {
    if (DEBUG_BUFFER) {
      System.out.print("(load, " + offset + ": ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition)
    var length = fCurrentEntity.ch.length - offset
    if (!fCurrentEntity.mayReadChunks &&
      length > XMLEntityManager.DEFAULT_XMLDECL_BUFFER_SIZE) {
      length = XMLEntityManager.DEFAULT_XMLDECL_BUFFER_SIZE
    }
    if (DEBUG_BUFFER) println("  length to try to read: " + length)
    val count = fCurrentEntity.reader.read(fCurrentEntity.ch, offset, length)
    if (DEBUG_BUFFER) println("  length actually read:  " + count)
    var entityChanged = false
    if (count != -1) {
      if (count != 0) {
        fCurrentEntity.count = count + offset
        fCurrentEntity.position = offset
        fCurrentEntity.startPosition = offset
      }
    } else {
      fCurrentEntity.count = offset
      fCurrentEntity.position = offset
      fCurrentEntity.startPosition = offset
      entityChanged = true
      if (changeEntity) {
        fEntityManager.endEntity()
        if (fCurrentEntity eq null) {
          throw END_OF_DOCUMENT_ENTITY
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
          load(0, changeEntity = true)
        }
      }
    }
    if (DEBUG_BUFFER) {
      System.out.print(")load, " + offset + ": ")
      XMLEntityManager.print(fCurrentEntity)
      println()
    }
    entityChanged
  }
}
