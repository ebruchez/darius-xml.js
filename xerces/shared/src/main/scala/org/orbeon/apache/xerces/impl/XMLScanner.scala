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

import org.orbeon.apache.xerces.impl.msg.XMLMessageFormatter
import org.orbeon.apache.xerces.util.HexUtils
import org.orbeon.apache.xerces.util.SymbolTable
import org.orbeon.apache.xerces.util.XMLChar
import org.orbeon.apache.xerces.util.XMLResourceIdentifierImpl
import org.orbeon.apache.xerces.util.XMLStringBuffer
import org.orbeon.apache.xerces.xni.Augmentations
import org.orbeon.apache.xerces.xni.XMLResourceIdentifier
import org.orbeon.apache.xerces.xni.XMLString
import org.orbeon.apache.xerces.xni.XNIException
import org.orbeon.apache.xerces.xni.parser.XMLComponent
import org.orbeon.apache.xerces.xni.parser.XMLComponentManager
import org.orbeon.apache.xerces.xni.parser.XMLConfigurationException

import scala.util.control.Breaks

protected[xerces] object XMLScanner {

  /**
   Feature identifier: validation.
   */
  protected[xerces] val VALIDATION = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE

  /**
   Feature identifier: namespaces.
   */
  val NAMESPACES = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE

  /**
   Feature identifier: notify character references.
   */
  val NOTIFY_CHAR_REFS = Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_CHAR_REFS_FEATURE

  val PARSER_SETTINGS = Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS

  /**
   Property identifier: symbol table.
   */
  val SYMBOL_TABLE = Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY

  /**
   Property identifier: error reporter.
   */
  val ERROR_REPORTER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY

  /**
   Property identifier: entity manager.
   */
  val ENTITY_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY

  /**
   Debug attribute normalization.
   */
  val DEBUG_ATTR_NORMALIZATION = false

  /**
   Symbol: "version".
   */
  val fVersionSymbol = "version".intern()

  /**
   Symbol: "encoding".
   */
  val fEncodingSymbol = "encoding".intern()

  /**
   Symbol: "standalone".
   */
  val fStandaloneSymbol = "standalone".intern()

  /**
   Symbol: "amp".
   */
  val fAmpSymbol = "amp".intern()

  /**
   Symbol: "lt".
   */
  val fLtSymbol = "lt".intern()

  /**
   Symbol: "gt".
   */
  val fGtSymbol = "gt".intern()

  /**
   Symbol: "quot".
   */
  val fQuotSymbol = "quot".intern()

  /**
   Symbol: "apos".
   */
  val fAposSymbol = "apos".intern()
}

/**
 * This class is responsible for holding scanning methods common to
 * scanning the XML document structure and content as well as the DTD
 * structure and content. Both XMLDocumentScanner and XMLDTDScanner inherit
 * from this base class.
 *
 *
 * This component requires the following features and properties from the
 * component manager that uses it:
 *
 *  - http://xml.org/sax/features/validation
 *  - http://xml.org/sax/features/namespaces
 *  - http://apache.org/xml/features/scanner/notify-char-refs
 *  - http://apache.org/xml/properties/internal/symbol-table
 *  - http://apache.org/xml/properties/internal/error-reporter
 *  - http://apache.org/xml/properties/internal/entity-manager
 *
 */
abstract class XMLScanner extends XMLComponent {

  import XMLScanner._

  /**
   * Validation. This feature identifier is:
   * http://xml.org/sax/features/validation
   */
  protected var fValidation: Boolean = false

  /**
   Namespaces.
   */
  protected var fNamespaces: Boolean = _

  /**
   Character references notification.
   */
  protected var fNotifyCharRefs: Boolean = false

  /**
   Internal parser-settings feature
   */
  protected var fParserSettings: Boolean = true

  /**
   Symbol table.
   */
  protected var fSymbolTable: SymbolTable = _

  /**
   Error reporter.
   */
  protected var fErrorReporter: XMLErrorReporter = _

  /**
   Entity manager.
   */
  protected var fEntityManager: XMLEntityManager = _

  /**
   Entity scanner.
   */
  protected var fEntityScanner: XMLEntityScanner = _

  /**
   Entity depth.
   */
  protected var fEntityDepth: Int = _

  /**
   Literal value of the last character refence scanned.
   */
  protected var fCharRefLiteral: String = null

  /**
   Scanning attribute.
   */
  protected var fScanningAttribute: Boolean = _

  /**
   Report entity boundary.
   */
  protected var fReportEntity: Boolean = _

  private val fString = new XMLString()
  private val fStringBuffer = new XMLStringBuffer()
  private val fStringBuffer2 = new XMLStringBuffer()
  private val fStringBuffer3 = new XMLStringBuffer()

  protected val fResourceIdentifier = new XMLResourceIdentifierImpl()

  def reset(componentManager: XMLComponentManager): Unit = {
    fParserSettings =
      try {
        componentManager.getFeature(PARSER_SETTINGS)
      } catch {
        case e: XMLConfigurationException => true
      }
		if (! fParserSettings) {
			init()
			return
		}
    fSymbolTable = componentManager.getProperty(SYMBOL_TABLE).asInstanceOf[SymbolTable]
    fErrorReporter = componentManager.getProperty(ERROR_REPORTER).asInstanceOf[XMLErrorReporter]
    fEntityManager = componentManager.getProperty(ENTITY_MANAGER).asInstanceOf[XMLEntityManager]
    try {
      fValidation = componentManager.getFeature(VALIDATION)
    } catch {
      case e: XMLConfigurationException => fValidation = false
    }
    try {
      fNamespaces = componentManager.getFeature(NAMESPACES)
    } catch {
      case e: XMLConfigurationException => fNamespaces = true
    }
    try {
      fNotifyCharRefs = componentManager.getFeature(NOTIFY_CHAR_REFS)
    } catch {
      case e: XMLConfigurationException => fNotifyCharRefs = false
    }
    init()
  }

  /**
   * Sets the value of a property during parsing.
   */
  def setProperty(propertyId: String, value: AnyRef): Unit = {
    if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.XERCES_PROPERTY_PREFIX.length
      if (suffixLength == Constants.SYMBOL_TABLE_PROPERTY.length &&
        propertyId.endsWith(Constants.SYMBOL_TABLE_PROPERTY)) {
        fSymbolTable = value.asInstanceOf[SymbolTable]
      } else if (suffixLength == Constants.ERROR_REPORTER_PROPERTY.length &&
        propertyId.endsWith(Constants.ERROR_REPORTER_PROPERTY)) {
        fErrorReporter = value.asInstanceOf[XMLErrorReporter]
      } else if (suffixLength == Constants.ENTITY_MANAGER_PROPERTY.length &&
        propertyId.endsWith(Constants.ENTITY_MANAGER_PROPERTY)) {
        fEntityManager = value.asInstanceOf[XMLEntityManager]
      }
    }
  }

  def setFeature(featureId: String, value: Boolean): Unit = {
    if (VALIDATION == featureId) {
      fValidation = value
    } else if (NOTIFY_CHAR_REFS == featureId) {
      fNotifyCharRefs = value
    }
  }

  def getFeature(featureId: String): Boolean = {
    if (VALIDATION == featureId) {
      return fValidation
    } else if (NOTIFY_CHAR_REFS == featureId) {
      return fNotifyCharRefs
    }
    throw new XMLConfigurationException(XMLConfigurationException.NOT_RECOGNIZED, featureId)
  }

  protected def reset(): Unit = {
    init()
    fValidation = true
    fNotifyCharRefs = false
  }

  /**
   * Scans an XML or text declaration.
   *
   *  [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
   *  [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
   *  [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
   *  [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
   *  [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
   *                  | ('"' ('yes' | 'no') '"'))
   *
   *  [77] TextDecl ::= '<?xml' VersionInfo? EncodingDecl S? '?>'
   *
   * @param scanningTextDecl True if a text declaration is to
   *                         be scanned instead of an XML
   *                         declaration.
   * @param pseudoAttributeValues An array of size 3 to return the version,
   *                         encoding and standalone pseudo attribute values
   *                         (in that order).
   *
   * *Note:* This method uses fString, anything in it
   * at the time of calling is lost.
   */
  protected def scanXMLDeclOrTextDecl(scanningTextDecl: Boolean, pseudoAttributeValues: Array[String]): Unit = {
    var version: String = null
    var encoding: String = null
    var standalone: String = null
    val STATE_VERSION = 0
    val STATE_ENCODING = 1
    val STATE_STANDALONE = 2
    val STATE_DONE = 3
    var state = STATE_VERSION
    var dataFoundForTarget = false
    var sawSpace = fEntityScanner.skipDeclSpaces()
    val currEnt = fEntityManager.getCurrentEntity
    val currLiteral = currEnt.literal
    currEnt.literal = false
    while (fEntityScanner.peekChar() != '?') {
      dataFoundForTarget = true
      val name = scanPseudoAttribute(scanningTextDecl, fString)
      state match {
        case STATE_VERSION =>
          if (name == fVersionSymbol) {
            if (!sawSpace) {
              reportFatalError(if (scanningTextDecl) "SpaceRequiredBeforeVersionInTextDecl" else "SpaceRequiredBeforeVersionInXMLDecl",
                null)
            }
            version = fString.toString
            state = STATE_ENCODING
            if (!versionSupported(version)) {
              reportFatalError(getVersionNotSupportedKey, Array(version))
            }
          } else if (name == fEncodingSymbol) {
            if (!scanningTextDecl) {
              reportFatalError("VersionInfoRequired", null)
            }
            if (!sawSpace) {
              reportFatalError(if (scanningTextDecl) "SpaceRequiredBeforeEncodingInTextDecl" else "SpaceRequiredBeforeEncodingInXMLDecl",
                null)
            }
            encoding = fString.toString
            state = if (scanningTextDecl) STATE_DONE else STATE_STANDALONE
          } else {
            if (scanningTextDecl) {
              reportFatalError("EncodingDeclRequired", null)
            } else {
              reportFatalError("VersionInfoRequired", null)
            }
          }
        case STATE_ENCODING =>
          if (name == fEncodingSymbol) {
            if (!sawSpace) {
              reportFatalError(if (scanningTextDecl) "SpaceRequiredBeforeEncodingInTextDecl" else "SpaceRequiredBeforeEncodingInXMLDecl",
                null)
            }
            encoding = fString.toString
            state = if (scanningTextDecl) STATE_DONE else STATE_STANDALONE
          } else if (!scanningTextDecl && name == fStandaloneSymbol) {
            if (!sawSpace) {
              reportFatalError("SpaceRequiredBeforeStandalone", null)
            }
            standalone = fString.toString
            state = STATE_DONE
            if (standalone != "yes" && standalone != "no") {
              reportFatalError("SDDeclInvalid", Array(standalone))
            }
          } else {
            reportFatalError("EncodingDeclRequired", null)
          }
        case STATE_STANDALONE =>
          if (name == fStandaloneSymbol) {
            if (!sawSpace) {
              reportFatalError("SpaceRequiredBeforeStandalone", null)
            }
            standalone = fString.toString
            state = STATE_DONE
            if (standalone != "yes" && standalone != "no") {
              reportFatalError("SDDeclInvalid", Array(standalone))
            }
          } else {
            reportFatalError("EncodingDeclRequired", null)
          }
        case _ =>
          reportFatalError("NoMorePseudoAttributes", null)
      }
      sawSpace = fEntityScanner.skipDeclSpaces()
    }
    if (currLiteral) currEnt.literal = true
    if (scanningTextDecl && state != STATE_DONE) {
      reportFatalError("MorePseudoAttributes", null)
    }
    if (scanningTextDecl) {
      if (!dataFoundForTarget && (encoding eq null)) {
        reportFatalError("EncodingDeclRequired", null)
      }
    } else {
      if (!dataFoundForTarget && (version eq null)) {
        reportFatalError("VersionInfoRequired", null)
      }
    }
    if (!fEntityScanner.skipChar('?')) {
      reportFatalError("XMLDeclUnterminated", null)
    }
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("XMLDeclUnterminated", null)
    }
    pseudoAttributeValues(0) = version
    pseudoAttributeValues(1) = encoding
    pseudoAttributeValues(2) = standalone
  }

  /**
   * Scans a pseudo attribute.
   *
   * @param scanningTextDecl True if scanning this pseudo-attribute for a
   *                         TextDecl; false if scanning XMLDecl. This
   *                         flag is needed to report the correct type of
   *                         error.
   * @param value            The string to fill in with the attribute
   *                         value.
   *
   * @return The name of the attribute
   *
   * *Note:* This method uses fStringBuffer2, anything in it
   * at the time of calling is lost.
   */
  def scanPseudoAttribute(scanningTextDecl: Boolean, value: XMLString): String = {
    val name = fEntityScanner.scanName()
    XMLEntityManager.print(fEntityManager.getCurrentEntity)
    if (name eq null) {
      reportFatalError("PseudoAttrNameExpected", null)
    }
    fEntityScanner.skipDeclSpaces()
    if (!fEntityScanner.skipChar('=')) {
      reportFatalError(if (scanningTextDecl) "EqRequiredInTextDecl" else "EqRequiredInXMLDecl", Array(name))
    }
    fEntityScanner.skipDeclSpaces()
    val quote = fEntityScanner.peekChar()
    if (quote != '\'' && quote != '"') {
      reportFatalError(if (scanningTextDecl) "QuoteRequiredInTextDecl" else "QuoteRequiredInXMLDecl",
        Array(name))
    }
    fEntityScanner.scanChar()
    var c = fEntityScanner.scanLiteral(quote, value)
    if (c != quote) {
      fStringBuffer2.clear()
      do {
        fStringBuffer2.append(value)
        if (c != -1) {
          if (c == '&' || c == '%' || c == '<' || c == ']') {
            fStringBuffer2.append(fEntityScanner.scanChar().toChar)
          } else if (XMLChar.isHighSurrogate(c)) {
            scanSurrogates(fStringBuffer2)
          } else if (isInvalidLiteral(c)) {
            val key = if (scanningTextDecl) "InvalidCharInTextDecl" else "InvalidCharInXMLDecl"
            reportFatalError(key, Array(HexUtils.toHexString(c)))
            fEntityScanner.scanChar()
          }
        }
        c = fEntityScanner.scanLiteral(quote, value)
      } while (c != quote)
      fStringBuffer2.append(value)
      value.setValues(fStringBuffer2)
    }
    if (!fEntityScanner.skipChar(quote)) {
      reportFatalError(if (scanningTextDecl) "CloseQuoteMissingInTextDecl" else "CloseQuoteMissingInXMLDecl",
        Array(name))
    }
    name
  }

  /**
   * Scans a processing instruction.
   *
   *  [16] PI ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'
   *  [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
   *
   * *Note:* This method uses fString, anything in it
   * at the time of calling is lost.
   */
  protected def scanPI(): Unit = {
    fReportEntity = false
    var target: String = null
    target = if (fNamespaces) fEntityScanner.scanNCName() else fEntityScanner.scanName()
    if (target eq null) {
      reportFatalError("PITargetRequired", null)
    }
    scanPIData(target, fString)
    fReportEntity = true
  }

  /**
   * Scans a processing data. This is needed to handle the situation
   * where a document starts with a processing instruction whose
   * target name *starts with* "xml". (e.g. xmlfoo)
   *
   * *Note:* This method uses fStringBuffer, anything in it
   * at the time of calling is lost.
   *
   * @param target The PI target
   * @param data The string to fill in with the data
   */
  protected def scanPIData(target: String, data: XMLString): Unit = {
    if (target.length == 3) {
      val c0 = Character.toLowerCase(target.charAt(0))
      val c1 = Character.toLowerCase(target.charAt(1))
      val c2 = Character.toLowerCase(target.charAt(2))
      if (c0 == 'x' && c1 == 'm' && c2 == 'l') {
        reportFatalError("ReservedPITarget", null)
      }
    }
    if (!fEntityScanner.skipSpaces()) {
      if (fEntityScanner.skipString("?>")) {
        data.clear()
        return
      } else {
        if (fNamespaces && fEntityScanner.peekChar() == ':') {
          fEntityScanner.scanChar()
          val colonName = new XMLStringBuffer(target)
          colonName.append(':')
          val str = fEntityScanner.scanName()
          if (str ne null) colonName.append(str)
          reportFatalError("ColonNotLegalWithNS", Array(colonName.toString))
          fEntityScanner.skipSpaces()
        } else {
          reportFatalError("SpaceRequiredInPI", null)
        }
      }
    }
    fStringBuffer.clear()
    if (fEntityScanner.scanData("?>", fStringBuffer)) {
      do {
        val c = fEntityScanner.peekChar()
        if (c != -1) {
          if (XMLChar.isHighSurrogate(c)) {
            scanSurrogates(fStringBuffer)
          } else if (isInvalidLiteral(c)) {
            reportFatalError("InvalidCharInPI", Array(Integer.toHexString(c)))
            fEntityScanner.scanChar()
          }
        }
      } while (fEntityScanner.scanData("?>", fStringBuffer))
    }
    data.setValues(fStringBuffer)
  }

  /**
   * Scans a comment.
   *
   *  [15] Comment ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
   *
   * *Note:* Called after scanning past '<!--'
   * *Note:* This method uses fString, anything in it
   * at the time of calling is lost.
   *
   * @param text The buffer to fill in with the text.
   */
  protected def scanComment(text: XMLStringBuffer): Unit = {
    text.clear()
    while (fEntityScanner.scanData("--", text)) {
      val c = fEntityScanner.peekChar()
      if (c != -1) {
        if (XMLChar.isHighSurrogate(c)) {
          scanSurrogates(text)
        } else if (isInvalidLiteral(c)) {
          reportFatalError("InvalidCharInComment", Array(Integer.toHexString(c)))
          fEntityScanner.scanChar()
        }
      }
    }
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("DashDashInComment", null)
    }
  }

  /**
   * Scans an attribute value and normalizes whitespace converting all
   * whitespace characters to space characters.
   *
   * [10] AttValue ::= '"' ([^<&"] | Reference)* '"' | "'" ([^<&'] | Reference)* "'"
   *
   * @param value The XMLString to fill in with the value.
   * @param nonNormalizedValue The XMLString to fill in with the
   *                           non-normalized value.
   * @param atName The name of the attribute being parsed (for error msgs).
   * @param checkEntities true if undeclared entities should be reported as VC violation,
   *                      false if undeclared entities should be reported as WFC violation.
   * @param eleName The name of element to which this attribute belongs.
   *
   * @return true if the non-normalized and normalized value are the same
   *
   * *Note:* This method uses fStringBuffer2, anything in it
   * at the time of calling is lost.
   *
   */
  protected def scanAttributeValue(value: XMLString,
      nonNormalizedValue: XMLString,
      atName: String,
      checkEntities: Boolean,
      eleName: String): Boolean = {
    val quote = fEntityScanner.peekChar()
    if (quote != '\'' && quote != '"') {
      reportFatalError("OpenQuoteExpected", Array(eleName, atName))
    }
    fEntityScanner.scanChar()
    val entityDepth = fEntityDepth
    var c = fEntityScanner.scanLiteral(quote, value)
    if (DEBUG_ATTR_NORMALIZATION) {
      println("** scanLiteral -> \"" + value.toString + "\"")
    }
    var fromIndex = 0
    if (c == quote && { fromIndex = isUnchangedByNormalization(value); fromIndex } == -1) {
      nonNormalizedValue.setValues(value)
      val cquote = fEntityScanner.scanChar()
      if (cquote != quote) {
        reportFatalError("CloseQuoteExpected", Array(eleName, atName))
      }
      return true
    }
    fStringBuffer2.clear()
    fStringBuffer2.append(value)
    normalizeWhitespace(value, fromIndex)
    if (DEBUG_ATTR_NORMALIZATION) {
      println("** normalizeWhitespace -> \"" + value.toString + "\"")
    }
    if (c != quote) {
      fScanningAttribute = true
      fStringBuffer.clear()
      do {
        fStringBuffer.append(value)
        if (DEBUG_ATTR_NORMALIZATION) {
          println("** value2: \"" + fStringBuffer.toString + "\"")
        }
        if (c == '&') {
          fEntityScanner.skipChar('&')
          if (entityDepth == fEntityDepth) {
            fStringBuffer2.append('&')
          }
          if (fEntityScanner.skipChar('#')) {
            if (entityDepth == fEntityDepth) {
              fStringBuffer2.append('#')
            }
            val ch = scanCharReferenceValue(fStringBuffer, fStringBuffer2)
            if (ch != -1) {
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** value3: \"" + fStringBuffer.toString + "\"")
              }
            }
          } else {
            val entityName = fEntityScanner.scanName()
            if (entityName eq null) {
              reportFatalError("NameRequiredInReference", null)
            } else if (entityDepth == fEntityDepth) {
              fStringBuffer2.append(entityName)
            }
            if (!fEntityScanner.skipChar(';')) {
              reportFatalError("SemicolonRequiredInReference", Array(entityName))
            } else if (entityDepth == fEntityDepth) {
              fStringBuffer2.append(';')
            }
            if (entityName == fAmpSymbol) {
              fStringBuffer.append('&')
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** value5: \"" + fStringBuffer.toString + "\"")
              }
            } else if (entityName == fAposSymbol) {
              fStringBuffer.append('\'')
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** value7: \"" + fStringBuffer.toString + "\"")
              }
            } else if (entityName == fLtSymbol) {
              fStringBuffer.append('<')
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** value9: \"" + fStringBuffer.toString + "\"")
              }
            } else if (entityName == fGtSymbol) {
              fStringBuffer.append('>')
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** valueB: \"" + fStringBuffer.toString + "\"")
              }
            } else if (entityName == fQuotSymbol) {
              fStringBuffer.append('"')
              if (DEBUG_ATTR_NORMALIZATION) {
                println("** valueD: \"" + fStringBuffer.toString + "\"")
              }
            } else {
              if (fEntityManager.isExternalEntity(entityName)) {
                reportFatalError("ReferenceToExternalEntity", Array(entityName))
              } else {
                if (!fEntityManager.isDeclaredEntity(entityName)) {
                  if (checkEntities) {
                    if (fValidation) {
                      fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EntityNotDeclared",
                        Array(entityName), XMLErrorReporter.SEVERITY_ERROR)
                    }
                  } else {
                    reportFatalError("EntityNotDeclared", Array(entityName))
                  }
                }
                fEntityManager.startEntity(entityName, literal = true)
              }
            }
          }
        } else if (c == '<') {
          reportFatalError("LessthanInAttValue", Array(eleName, atName))
          fEntityScanner.scanChar()
          if (entityDepth == fEntityDepth) {
            fStringBuffer2.append(c.toChar)
          }
        } else if (c == '%' || c == ']') {
          fEntityScanner.scanChar()
          fStringBuffer.append(c.toChar)
          if (entityDepth == fEntityDepth) {
            fStringBuffer2.append(c.toChar)
          }
          if (DEBUG_ATTR_NORMALIZATION) {
            println("** valueF: \"" + fStringBuffer.toString + "\"")
          }
        } else if (c == '\n' || c == '\r') {
          fEntityScanner.scanChar()
          fStringBuffer.append(' ')
          if (entityDepth == fEntityDepth) {
            fStringBuffer2.append('\n')
          }
        } else if (c != -1 && XMLChar.isHighSurrogate(c)) {
          fStringBuffer3.clear()
          if (scanSurrogates(fStringBuffer3)) {
            fStringBuffer.append(fStringBuffer3)
            if (entityDepth == fEntityDepth) {
              fStringBuffer2.append(fStringBuffer3)
            }
            if (DEBUG_ATTR_NORMALIZATION) {
              println("** valueI: \"" + fStringBuffer.toString + "\"")
            }
          }
        } else if (c != -1 && isInvalidLiteral(c)) {
          reportFatalError("InvalidCharInAttValue", Array(eleName, atName, HexUtils.toHexString(c)))
          fEntityScanner.scanChar()
          if (entityDepth == fEntityDepth) {
            fStringBuffer2.append(c.toChar)
          }
        }
        c = fEntityScanner.scanLiteral(quote, value)
        if (entityDepth == fEntityDepth) {
          fStringBuffer2.append(value)
        }
        normalizeWhitespace(value)
      } while (c != quote || entityDepth != fEntityDepth)
      fStringBuffer.append(value)
      if (DEBUG_ATTR_NORMALIZATION) {
        println("** valueN: \"" + fStringBuffer.toString + "\"")
      }
      value.setValues(fStringBuffer)
      fScanningAttribute = false
    }
    nonNormalizedValue.setValues(fStringBuffer2)
    val cquote = fEntityScanner.scanChar()
    if (cquote != quote) {
      reportFatalError("CloseQuoteExpected", Array(eleName, atName))
    }
    nonNormalizedValue.==(value.ch, value.offset, value.length)
  }

  /**
   * Scans External ID and return the public and system IDs.
   *
   * @param identifiers An array of size 2 to return the system id,
   *                    and public id (in that order).
   * @param optionalSystemId Specifies whether the system id is optional.
   *
   * *Note:* This method uses fString and fStringBuffer,
   * anything in them at the time of calling is lost.
   */
  protected def scanExternalID(identifiers: Array[String], optionalSystemId: Boolean): Unit = {
    var systemId: String = null
    var publicId: String = null
    if (fEntityScanner.skipString("PUBLIC")) {
      if (!fEntityScanner.skipSpaces()) {
        reportFatalError("SpaceRequiredAfterPUBLIC", null)
      }
      scanPubidLiteral(fString)
      publicId = fString.toString
      if (!fEntityScanner.skipSpaces() && !optionalSystemId) {
        reportFatalError("SpaceRequiredBetweenPublicAndSystem", null)
      }
    }
    if ((publicId ne null) || fEntityScanner.skipString("SYSTEM")) {
      if ((publicId eq null) && !fEntityScanner.skipSpaces()) {
        reportFatalError("SpaceRequiredAfterSYSTEM", null)
      }
      val quote = fEntityScanner.peekChar()
      if (quote != '\'' && quote != '"') {
        if ((publicId ne null) && optionalSystemId) {
          identifiers(0) = null
          identifiers(1) = publicId
          return
        }
        reportFatalError("QuoteRequiredInSystemID", null)
      }
      fEntityScanner.scanChar()
      var ident = fString
      if (fEntityScanner.scanLiteral(quote, ident) != quote) {
        fStringBuffer.clear()
        do {
          fStringBuffer.append(ident)
          val c = fEntityScanner.peekChar()
          if (XMLChar.isMarkup(c) || c == ']') {
            fStringBuffer.append(fEntityScanner.scanChar().toChar)
          } else if (XMLChar.isHighSurrogate(c)) {
            scanSurrogates(fStringBuffer)
          } else if (isInvalidLiteral(c)) {
            reportFatalError("InvalidCharInSystemID", Array(Integer.toHexString(c)))
            fEntityScanner.scanChar()
          }
        } while (fEntityScanner.scanLiteral(quote, ident) != quote)
        fStringBuffer.append(ident)
        ident = fStringBuffer
      }
      systemId = ident.toString
      if (!fEntityScanner.skipChar(quote)) {
        reportFatalError("SystemIDUnterminated", null)
      }
    }
    identifiers(0) = systemId
    identifiers(1) = publicId
  }

  /**
   * Scans public ID literal.
   *
   * [12] PubidLiteral ::= '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"
   * [13] PubidChar::= #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
   *
   * The returned string is normalized according to the following rule,
   * from http://www.w3.org/TR/REC-xml#dt-pubid:
   *
   * Before a match is attempted, all strings of white space in the public
   * identifier must be normalized to single space characters (#x20), and
   * leading and trailing white space must be removed.
   *
   * @param literal The string to fill in with the public ID literal.
   * @return True on success.
   *
   * *Note:* This method uses fStringBuffer, anything in it at
   * the time of calling is lost.
   */
  protected def scanPubidLiteral(literal: XMLString): Boolean = {
    val quote = fEntityScanner.scanChar()
    if (quote != '\'' && quote != '"') {
      reportFatalError("QuoteRequiredInPublicID", null)
      return false
    }
    fStringBuffer.clear()
    var skipSpace = true
    var dataok = true
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      while (true) {
        val c = fEntityScanner.scanChar()
        if (c == ' ' || c == '\n' || c == '\r') {
          if (!skipSpace) {
            fStringBuffer.append(' ')
            skipSpace = true
          }
        } else if (c == quote) {
          if (skipSpace) {
            fStringBuffer.length -= 1
          }
          literal.setValues(fStringBuffer)
          whileBreaks.break()
        } else if (XMLChar.isPubid(c)) {
          fStringBuffer.append(c.toChar)
          skipSpace = false
        } else if (c == -1) {
          reportFatalError("PublicIDUnterminated", null)
          return false
        } else {
          dataok = false
          reportFatalError("InvalidCharInPublicID", Array(Integer.toHexString(c)))
        }
      }
    }
    dataok
  }

  /**
   * Normalize whitespace in an XMLString converting all whitespace
   * characters to space characters.
   */
  protected def normalizeWhitespace(value: XMLString): Unit = {
    val end = value.offset + value.length
    for (i <- value.offset until end) {
      val c = value.ch(i)
      if (c < 0x20) {
        value.ch(i) = ' '
      }
    }
  }

  /**
   * Normalize whitespace in an XMLString converting all whitespace
   * characters to space characters.
   */
  protected def normalizeWhitespace(value: XMLString, fromIndex: Int): Unit = {
    val end = value.offset + value.length
    for (i <- value.offset + fromIndex until end) {
      val c = value.ch(i)
      if (c < 0x20) {
        value.ch(i) = ' '
      }
    }
  }

  /**
   * Checks whether this string would be unchanged by normalization.
   *
   * @return -1 if the value would be unchanged by normalization,
   * otherwise the index of the first whitespace character which
   * would be transformed.
   */
  protected def isUnchangedByNormalization(value: XMLString): Int = {
    val end = value.offset + value.length
    for (i <- value.offset until end) {
      val c = value.ch(i)
      if (c < 0x20) {
        return i - value.offset
      }
    }
    -1
  }

  /**
   * This method notifies of the start of an entity. The document entity
   * has the pseudo-name of "[xml]" the DTD has the pseudo-name of "[dtd]"
   * parameter entity names start with '%'; and general entities are just
   * specified by their name.
   *
   * @param name     The name of the entity.
   * @param identifier The resource identifier.
   * @param encoding The auto-detected IANA encoding name of the entity
   *                 stream. This value will be null in those situations
   *                 where the entity encoding is not auto-detected (e.g.
   *                 internal entities or a document entity that is
   *                 parsed from a java.io.Reader).
   * @param augs     Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startEntity(name: String,
      identifier: XMLResourceIdentifier,
      encoding: String,
      augs: Augmentations): Unit = {
    fEntityDepth += 1
    fEntityScanner = fEntityManager.getEntityScanner
  }

  /**
   * This method notifies the end of an entity. The document entity has
   * the pseudo-name of "[xml]" the DTD has the pseudo-name of "[dtd]"
   * parameter entity names start with '%'; and general entities are just
   * specified by their name.
   *
   * @param name The name of the entity.
   * @param augs Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endEntity(name: String, augs: Augmentations): Unit = {
    fEntityDepth -= 1
  }

  /**
   * Scans a character reference and append the corresponding chars to the
   * specified buffer.
   *
   *  [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
   *
   * *Note:* This method uses fStringBuffer, anything in it
   * at the time of calling is lost.
   *
   * @param buf the character buffer to append chars to
   * @param buf2 the character buffer to append non-normalized chars to
   *
   * @return the character value or (-1) on conversion failure
   */
  protected def scanCharReferenceValue(buf: XMLStringBuffer, buf2: XMLStringBuffer): Int = {
    var hex = false
    if (fEntityScanner.skipChar('x')) {
      if (buf2 ne null) {
        buf2.append('x')
      }
      hex = true
      fStringBuffer3.clear()
      var digit = true
      var c = fEntityScanner.peekChar()
      digit = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
      if (digit) {
        if (buf2 ne null) {
          buf2.append(c.toChar)
        }
        fEntityScanner.scanChar()
        fStringBuffer3.append(c.toChar)
        do {
          c = fEntityScanner.peekChar()
          digit = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
          if (digit) {
            if (buf2 ne null) {
              buf2.append(c.toChar)
            }
            fEntityScanner.scanChar()
            fStringBuffer3.append(c.toChar)
          }
        } while (digit)
      } else {
        reportFatalError("HexdigitRequiredInCharRef", null)
      }
    } else {
      fStringBuffer3.clear()
      var digit = true
      var c = fEntityScanner.peekChar()
      digit = c >= '0' && c <= '9'
      if (digit) {
        if (buf2 ne null) {
          buf2.append(c.toChar)
        }
        fEntityScanner.scanChar()
        fStringBuffer3.append(c.toChar)
        do {
          c = fEntityScanner.peekChar()
          digit = c >= '0' && c <= '9'
          if (digit) {
            if (buf2 ne null) {
              buf2.append(c.toChar)
            }
            fEntityScanner.scanChar()
            fStringBuffer3.append(c.toChar)
          }
        } while (digit)
      } else {
        reportFatalError("DigitRequiredInCharRef", null)
      }
    }
    if (!fEntityScanner.skipChar(';')) {
      reportFatalError("SemicolonRequiredInCharRef", null)
    }
    if (buf2 ne null) {
      buf2.append(';')
    }
    var value = -1
    try {
      value = Integer.parseInt(fStringBuffer3.toString, if (hex) 16 else 10)
      if (isInvalid(value)) {
        val errorBuf = new StringBuffer(fStringBuffer3.length + 1)
        if (hex) errorBuf.append('x')
        errorBuf.append(fStringBuffer3.ch, fStringBuffer3.offset, fStringBuffer3.length)
        reportFatalError("InvalidCharRef", Array(errorBuf.toString))
      }
    } catch {
      case e: NumberFormatException =>
        val errorBuf = new StringBuffer(fStringBuffer3.length + 1)
        if (hex) errorBuf.append('x')
        errorBuf.append(fStringBuffer3.ch, fStringBuffer3.offset, fStringBuffer3.length)
        reportFatalError("InvalidCharRef", Array(errorBuf.toString))
    }
    if (!XMLChar.isSupplemental(value)) {
      buf.append(value.toChar)
    } else {
      buf.append(XMLChar.highSurrogate(value))
      buf.append(XMLChar.lowSurrogate(value))
    }
    if (fNotifyCharRefs && value != -1) {
      val literal = "#" + (if (hex) "x" else "") + fStringBuffer3.toString
      if (!fScanningAttribute) {
        fCharRefLiteral = literal
      }
    }
    value
  }

  protected def isInvalid(value: Int): Boolean = XMLChar.isInvalid(value)

  protected def isInvalidLiteral(value: Int): Boolean = XMLChar.isInvalid(value)

  protected def isValidNameChar(value: Int): Boolean = XMLChar.isName(value)

  protected def isValidNameStartChar(value: Int): Boolean = XMLChar.isNameStart(value)

  protected def isValidNCName(value: Int): Boolean = XMLChar.isNCName(value)

  protected def isValidNameStartHighSurrogate(value: Int): Boolean = false

  protected def versionSupported(version: String): Boolean = version == "1.0"

  protected def getVersionNotSupportedKey: String = "VersionNotSupported"

  /**
   * Scans surrogates and append them to the specified buffer.
   *
   * *Note:* This assumes the current char has already been
   * identified as a high surrogate.
   *
   * @param buf The StringBuffer to append the read surrogates to.
   * @return True if it succeeded.
   */
  protected def scanSurrogates(buf: XMLStringBuffer): Boolean = {
    val high = fEntityScanner.scanChar()
    val low = fEntityScanner.peekChar()
    if (!XMLChar.isLowSurrogate(low)) {
      reportFatalError("InvalidCharInContent", Array(HexUtils.toHexString(high)))
      return false
    }
    fEntityScanner.scanChar()
    val c = XMLChar.supplemental(high.toChar, low.toChar)
    if (isInvalid(c)) {
      reportFatalError("InvalidCharInContent", Array(HexUtils.toHexString(c)))
      return false
    }
    buf.append(high.toChar)
    buf.append(low.toChar)
    true
  }

  /**
   * Convenience function used in all XML scanners.
   */
  protected def reportFatalError(msgId: String, args: Array[Any]): Unit = {
    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, msgId, args, XMLErrorReporter.SEVERITY_FATAL_ERROR)
  }

  private def init(): Unit = {
    fEntityScanner = null
    fEntityDepth = 0
    fReportEntity = true
    fResourceIdentifier.clear()
  }
}
