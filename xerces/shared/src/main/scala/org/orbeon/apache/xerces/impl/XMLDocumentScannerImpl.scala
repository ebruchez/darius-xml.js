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

import org.orbeon.apache.xerces.impl.XMLDocumentScannerImpl._
import org.orbeon.apache.xerces.impl.dtd.XMLDTDDescription
import org.orbeon.apache.xerces.impl.io.MalformedByteSequenceException
import org.orbeon.apache.xerces.impl.validation.ValidationManager
import org.orbeon.apache.xerces.util.NamespaceSupport
import org.orbeon.apache.xerces.util.XMLChar
import org.orbeon.apache.xerces.util.XMLStringBuffer
import org.orbeon.apache.xerces.xni.Augmentations
import org.orbeon.apache.xerces.xni.NamespaceContext
import org.orbeon.apache.xerces.xni.XMLResourceIdentifier
import org.orbeon.apache.xerces.xni.XMLString
import org.orbeon.apache.xerces.xni.XNIException
import org.orbeon.apache.xerces.xni.parser.XMLComponentManager
import org.orbeon.apache.xerces.xni.parser.XMLConfigurationException
import org.orbeon.apache.xerces.xni.parser.XMLDTDScanner
import org.orbeon.apache.xerces.xni.parser.XMLInputSource

import scala.util.control.Breaks

protected[impl] object XMLDocumentScannerImpl {

  /**
   Scanner state: XML declaration.
   */
  val SCANNER_STATE_XML_DECL = 0

  /**
   Scanner state: prolog.
   */
  val SCANNER_STATE_PROLOG = 5

  /**
   Scanner state: trailing misc.
   */
  val SCANNER_STATE_TRAILING_MISC = 12

  /**
   Scanner state: DTD internal declarations.
   */
  val SCANNER_STATE_DTD_INTERNAL_DECLS = 17

  /**
   Scanner state: open DTD external subset.
   */
  val SCANNER_STATE_DTD_EXTERNAL = 18

  /**
   Scanner state: DTD external declarations.
   */
  val SCANNER_STATE_DTD_EXTERNAL_DECLS = 19

  /**
   Feature identifier: load external DTD.
   */
  val LOAD_EXTERNAL_DTD = Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE

  /**
   Feature identifier: load external DTD.
   */
  val DISALLOW_DOCTYPE_DECL_FEATURE = Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE

  /**
   Property identifier: DTD scanner.
   */
  val DTD_SCANNER = Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_SCANNER_PROPERTY

  /**
   property identifier:  ValidationManager
   */
  val VALIDATION_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY

  /**
   property identifier:  NamespaceContext
   */
  val NAMESPACE_CONTEXT = Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_CONTEXT_PROPERTY

  /**
   Recognized features.
   */
  private val RECOGNIZED_FEATURES = Array(LOAD_EXTERNAL_DTD, DISALLOW_DOCTYPE_DECL_FEATURE)

  /**
   Feature defaults.
   */
  private val FEATURE_DEFAULTS = Array[java.lang.Boolean](true, false)

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(DTD_SCANNER, VALIDATION_MANAGER, NAMESPACE_CONTEXT)

  /**
   Property defaults.
   */
  private val PROPERTY_DEFAULTS = Array(null, null, null)
}

/**
 * This class is responsible for scanning XML document structure
 * and content. The scanner acts as the source for the document
 * information which is communicated to the document handler.
 *
 * This component requires the following features and properties from the
 * component manager that uses it:
 *
 *  - http://xml.org/sax/features/namespaces
 *  - http://xml.org/sax/features/validation
 *  - http://apache.org/xml/features/nonvalidating/load-external-dtd
 *  - http://apache.org/xml/features/scanner/notify-char-refs
 *  - http://apache.org/xml/features/scanner/notify-builtin-refs
 *  - http://apache.org/xml/properties/internal/symbol-table
 *  - http://apache.org/xml/properties/internal/error-reporter
 *  - http://apache.org/xml/properties/internal/entity-manager
 *  - http://apache.org/xml/properties/internal/dtd-scanner
 *
 */
class XMLDocumentScannerImpl extends XMLDocumentFragmentScannerImpl {

  import XMLDocumentFragmentScannerImpl._

  /**
   DTD scanner.
   */
  protected var fDTDScanner: XMLDTDScanner = _

  /**
   Validation manager .
   */
  protected var fValidationManager: ValidationManager = _

  /**
   Scanning DTD.
   */
  protected var fScanningDTD: Boolean = _

  /**
   Doctype name.
   */
  protected var fDoctypeName: String = _

  /**
   Doctype declaration public identifier.
   */
  protected var fDoctypePublicId: String = _

  /**
   Doctype declaration system identifier.
   */
  protected var fDoctypeSystemId: String = _

  /**
   Namespace support.
   */
  protected var fNamespaceContext: NamespaceContext = new NamespaceSupport()

  /**
   Load external DTD.
   */
  protected var fLoadExternalDTD: Boolean = true

  /**
   Disallow doctype declaration.
   */
  protected var fDisallowDoctype: Boolean = false

  /**
   Seen doctype declaration.
   */
  protected var fSeenDoctypeDecl: Boolean = _

  /**
   XML declaration dispatcher.
   */
  protected val fXMLDeclDispatcher = new XMLDeclDispatcher()

  /**
   Prolog dispatcher.
   */
  protected val fPrologDispatcher = new PrologDispatcher()

  /**
   DTD dispatcher.
   */
  protected val fDTDDispatcher = new DTDDispatcher()

  /**
   Trailing miscellaneous section dispatcher.
   */
  protected val fTrailingMiscDispatcher = new TrailingMiscDispatcher()

  /**
   Array of 3 strings.
   */
  private val fStrings = new Array[String](3)

  /**
   String.
   */
  private val fString = new XMLString()

  /**
   String buffer.
   */
  private val fStringBuffer = new XMLStringBuffer()

  /**
   External subset source.
   */
  private var fExternalSubsetSource: XMLInputSource = null

  /**
   A DTD Description.
   */
  private val fDTDDescription = new XMLDTDDescription(null, null, null, null, null)

  /**
   * Sets the input source.
   *
   * @param inputSource The input source.
   *
   * @throws IOException Thrown on i/o error.
   */
  override def setInputSource(inputSource: XMLInputSource): Unit = {
    fEntityManager.setEntityHandler(this)
    fEntityManager.startDocumentEntity(inputSource)
  }

  /**
   * Resets the component. The component can query the component manager
   * about any features and properties that affect the operation of the
   * component.
   */
  override def reset(componentManager: XMLComponentManager): Unit = {
    super.reset(componentManager)
    fDoctypeName = null
    fDoctypePublicId = null
    fDoctypeSystemId = null
    fSeenDoctypeDecl = false
    fScanningDTD = false
    fExternalSubsetSource = null
    if (! fParserSettings) {
      fNamespaceContext.reset()
      setScannerState(SCANNER_STATE_XML_DECL)
      setDispatcher(fXMLDeclDispatcher)
      return
    }
    try {
      fLoadExternalDTD = componentManager.getFeature(LOAD_EXTERNAL_DTD)
    } catch {
      case e: XMLConfigurationException => fLoadExternalDTD = true
    }
    try {
      fDisallowDoctype = componentManager.getFeature(DISALLOW_DOCTYPE_DECL_FEATURE)
    } catch {
      case e: XMLConfigurationException => fDisallowDoctype = false
    }
    fDTDScanner = componentManager.getProperty(DTD_SCANNER).asInstanceOf[XMLDTDScanner]
    try {
      fValidationManager = componentManager.getProperty(VALIDATION_MANAGER).asInstanceOf[ValidationManager]
    } catch {
      case e: XMLConfigurationException => fValidationManager = null
    }
    try {
      fNamespaceContext = componentManager.getProperty(NAMESPACE_CONTEXT).asInstanceOf[NamespaceContext]
    } catch {
      case e: XMLConfigurationException =>
    }
    if (fNamespaceContext eq null) {
      fNamespaceContext = new NamespaceSupport()
    }
    fNamespaceContext.reset()
    setScannerState(SCANNER_STATE_XML_DECL)
    setDispatcher(fXMLDeclDispatcher)
  }

  /**
   * Returns a list of feature identifiers that are recognized by
   * this component. This method may return null if no features
   * are recognized by this component.
   */
  override def getRecognizedFeatures: Array[String] = {
    val featureIds = super.getRecognizedFeatures
    val length = if (featureIds ne null) featureIds.length else 0
    val combinedFeatureIds = new Array[String](length + RECOGNIZED_FEATURES.length)
    if (featureIds ne null) {
      System.arraycopy(featureIds, 0, combinedFeatureIds, 0, featureIds.length)
    }
    System.arraycopy(RECOGNIZED_FEATURES, 0, combinedFeatureIds, length, RECOGNIZED_FEATURES.length)
    combinedFeatureIds
  }

  /**
   * Sets the state of a feature. This method is called by the component
   * manager any time after reset when a feature changes state.
   *
   * *Note:* Components should silently ignore features
   * that do not affect the operation of the component.
   */
  override def setFeature(featureId: String, state: Boolean): Unit = {
    super.setFeature(featureId, state)
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length &&
        featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
        fLoadExternalDTD = state
        return
      } else if (suffixLength == Constants.DISALLOW_DOCTYPE_DECL_FEATURE.length &&
        featureId.endsWith(Constants.DISALLOW_DOCTYPE_DECL_FEATURE)) {
        fDisallowDoctype = state
        return
      }
    }
  }

  /**
   * Returns a list of property identifiers that are recognized by
   * this component. This method may return null if no properties
   * are recognized by this component.
   */
  override def getRecognizedProperties: Array[String] = {
    val propertyIds = super.getRecognizedProperties
    val length = if (propertyIds ne null) propertyIds.length else 0
    val combinedPropertyIds = new Array[String](length + RECOGNIZED_PROPERTIES.length)
    if (propertyIds ne null) {
      System.arraycopy(propertyIds, 0, combinedPropertyIds, 0, propertyIds.length)
    }
    System.arraycopy(RECOGNIZED_PROPERTIES, 0, combinedPropertyIds, length, RECOGNIZED_PROPERTIES.length)
    combinedPropertyIds
  }

  /**
   * Sets the value of a property. This method is called by the component
   * manager any time after reset when a property changes value.
   *
   * *Note:* Components should silently ignore properties
   * that do not affect the operation of the component.
   */
  override def setProperty(propertyId: String, value: AnyRef): Unit = {
    super.setProperty(propertyId, value)
    if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.XERCES_PROPERTY_PREFIX.length
      if (suffixLength == Constants.DTD_SCANNER_PROPERTY.length &&
        propertyId.endsWith(Constants.DTD_SCANNER_PROPERTY)) {
        fDTDScanner = value.asInstanceOf[XMLDTDScanner]
      }
      if (suffixLength == Constants.NAMESPACE_CONTEXT_PROPERTY.length &&
        propertyId.endsWith(Constants.NAMESPACE_CONTEXT_PROPERTY)) {
        if (value ne null) {
          fNamespaceContext = value.asInstanceOf[NamespaceContext]
        }
      }
      return
    }
  }

  /**
   * Returns the default state for a feature, or null if this
   * component does not want to report a default value for this
   * feature.
   */
  override def getFeatureDefault(featureId: String): java.lang.Boolean = {
    RECOGNIZED_FEATURES.indices.find(RECOGNIZED_FEATURES(_) == featureId)
      .map(FEATURE_DEFAULTS(_))
      .getOrElse(super.getFeatureDefault(featureId))
  }

  /**
   * Returns the default state for a property, or null if this
   * component does not want to report a default value for this
   * property.
   */
  override def getPropertyDefault(propertyId: String): AnyRef = {
    RECOGNIZED_PROPERTIES.indices.find(RECOGNIZED_PROPERTIES(_) == propertyId)
      .map(PROPERTY_DEFAULTS(_))
      .getOrElse(super.getPropertyDefault(propertyId))
  }

  /**
   * This method notifies of the start of an entity. The DTD has the
   * pseudo-name of "[dtd]" parameter entity names start with '%'; and
   * general entities are just specified by their name.
   *
   * @param name     The name of the entity.
   * @param identifier The resource identifier.
   * @param encoding The auto-detected IANA encoding name of the entity
   *                 stream. This value will be null in those situations
   *                 where the entity encoding is not auto-detected (e.g.
   *                 internal entities or a document entity that is
   *                 parsed from a java.io.Reader).
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  override def startEntity(name: String,
      identifier: XMLResourceIdentifier,
      encoding: String,
      augs: Augmentations): Unit = {
    super.startEntity(name, identifier, encoding, augs)
    if (name != "[xml]" && fEntityScanner.isExternal) {
      setScannerState(SCANNER_STATE_TEXT_DECL)
    }
    if ((fDocumentHandler ne null) && name == "[xml]") {
      fDocumentHandler.startDocument(fEntityScanner, encoding, fNamespaceContext, null)
    }
  }

  /**
   * This method notifies the end of an entity. The DTD has the pseudo-name
   * of "[dtd]" parameter entity names start with '%'; and general entities
   * are just specified by their name.
   *
   * @param name The name of the entity.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  override def endEntity(name: String, augs: Augmentations): Unit = {
    super.endEntity(name, augs)
    if ((fDocumentHandler ne null) && name == "[xml]") {
      fDocumentHandler.endDocument(null)
    }
  }

  /**
   Creates a content dispatcher.
   */
  override protected def createContentDispatcher(): Dispatcher = new ContentDispatcher()

  /**
   Scans a doctype declaration.
   */
  protected def scanDoctypeDecl(): Boolean = {
    if (!fEntityScanner.skipSpaces()) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL", null)
    }
    fDoctypeName = fEntityScanner.scanName()
    if (fDoctypeName eq null) {
      reportFatalError("MSG_ROOT_ELEMENT_TYPE_REQUIRED", null)
    }
    if (fEntityScanner.skipSpaces()) {
      scanExternalID(fStrings, optionalSystemId = false)
      fDoctypeSystemId = fStrings(0)
      fDoctypePublicId = fStrings(1)
      fEntityScanner.skipSpaces()
    }
    fHasExternalDTD = fDoctypeSystemId ne null
    if (! fHasExternalDTD && (fExternalSubsetResolver ne null)) {
      fDTDDescription.setValues(null, null, fEntityManager.getCurrentResourceIdentifier.getExpandedSystemId,
        null)
      fDTDDescription.setRootName(fDoctypeName)
      fExternalSubsetSource = fExternalSubsetResolver.getExternalSubset(fDTDDescription)
      fHasExternalDTD = fExternalSubsetSource ne null
    }
    if (fDocumentHandler ne null) {
      if (fExternalSubsetSource eq null) {
        fDocumentHandler.doctypeDecl(fDoctypeName, fDoctypePublicId, fDoctypeSystemId, null)
      } else {
        fDocumentHandler.doctypeDecl(fDoctypeName, fExternalSubsetSource.getPublicId, fExternalSubsetSource.getSystemId,
          null)
      }
    }
    var internalSubset = true
    if (!fEntityScanner.skipChar('[')) {
      internalSubset = false
      fEntityScanner.skipSpaces()
      if (!fEntityScanner.skipChar('>')) {
        reportFatalError("DoctypedeclUnterminated", Array(fDoctypeName))
      }
      fMarkupDepth -= 1
    }
    internalSubset
  }

  /**
   Returns the scanner state name.
   */
  override protected def getScannerStateName(state: Int): String = state match {
    case SCANNER_STATE_XML_DECL => "SCANNER_STATE_XML_DECL"
    case SCANNER_STATE_PROLOG => "SCANNER_STATE_PROLOG"
    case SCANNER_STATE_TRAILING_MISC => "SCANNER_STATE_TRAILING_MISC"
    case SCANNER_STATE_DTD_INTERNAL_DECLS => "SCANNER_STATE_DTD_INTERNAL_DECLS"
    case SCANNER_STATE_DTD_EXTERNAL => "SCANNER_STATE_DTD_EXTERNAL"
    case SCANNER_STATE_DTD_EXTERNAL_DECLS => "SCANNER_STATE_DTD_EXTERNAL_DECLS"
  }

  /**
   * Dispatcher to handle XMLDecl scanning.
   */
  protected class XMLDeclDispatcher extends Dispatcher {

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this dispatcher is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if there is more to dispatch either from this
     *          or a another dispatcher.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     */
    def dispatch(complete: Boolean): Boolean = {
      setScannerState(SCANNER_STATE_PROLOG)
      setDispatcher(fPrologDispatcher)
      try {
        if (fEntityScanner.skipString("<?xml")) {
          fMarkupDepth += 1
          if (XMLChar.isName(fEntityScanner.peekChar())) {
            fStringBuffer.clear()
            fStringBuffer.append("xml")
            if (fNamespaces) {
              while (XMLChar.isNCName(fEntityScanner.peekChar())) {
                fStringBuffer.append(fEntityScanner.scanChar().toChar)
              }
            } else {
              while (XMLChar.isName(fEntityScanner.peekChar())) {
                fStringBuffer.append(fEntityScanner.scanChar().toChar)
              }
            }
            val target = fSymbolTable.addSymbol(fStringBuffer.ch, fStringBuffer.offset, fStringBuffer.length)
            scanPIData(target, fString)
          } else {
            scanXMLDeclOrTextDecl(scanningTextDecl = false)
          }
        }
        fEntityManager.fCurrentEntity.mayReadChunks = true
        true
      } catch {
        case e: MalformedByteSequenceException =>
          fErrorReporter.reportError(e.getDomain, e.getKey, e.getArguments, XMLErrorReporter.SEVERITY_FATAL_ERROR,
            e)
          false
// @ebruchez: not supported in Scala.js
//        case e: CharConversionException =>
//          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR,
//            e)
//          false
        case e: EOFException =>
          reportFatalError("PrematureEOF", null)
          false
      }
    }
  }

  /**
   * Dispatcher to handle prolog scanning.
   */
  protected class PrologDispatcher extends Dispatcher {

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this dispatcher is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if there is more to dispatch either from this
     *          or a another dispatcher.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     */
    def dispatch(complete: Boolean): Boolean = {
      try {
        var again: Boolean = false
        do {
          again = false
          fScannerState match {
            case SCANNER_STATE_PROLOG =>
              fEntityScanner.skipSpaces()
              if (fEntityScanner.skipChar('<')) {
                setScannerState(SCANNER_STATE_START_OF_MARKUP)
                again = true
              } else if (fEntityScanner.skipChar('&')) {
                setScannerState(SCANNER_STATE_REFERENCE)
                again = true
              } else {
                setScannerState(SCANNER_STATE_CONTENT)
                again = true
              }
            case SCANNER_STATE_START_OF_MARKUP =>
              fMarkupDepth += 1
              if (fEntityScanner.skipChar('!')) {
                if (fEntityScanner.skipChar('-')) {
                  if (!fEntityScanner.skipChar('-')) {
                    reportFatalError("InvalidCommentStart", null)
                  }
                  setScannerState(SCANNER_STATE_COMMENT)
                  again = true
                } else if (fEntityScanner.skipString("DOCTYPE")) {
                  setScannerState(SCANNER_STATE_DOCTYPE)
                  again = true
                } else {
                  reportFatalError("MarkupNotRecognizedInProlog", null)
                }
              } else if (isValidNameStartChar(fEntityScanner.peekChar())) {
                setScannerState(SCANNER_STATE_ROOT_ELEMENT)
                setDispatcher(fContentDispatcher)
                return true
              } else if (fEntityScanner.skipChar('?')) {
                setScannerState(SCANNER_STATE_PI)
                again = true
              } else if (isValidNameStartHighSurrogate(fEntityScanner.peekChar())) {
                setScannerState(SCANNER_STATE_ROOT_ELEMENT)
                setDispatcher(fContentDispatcher)
                return true
              } else {
                reportFatalError("MarkupNotRecognizedInProlog", null)
              }
            case SCANNER_STATE_COMMENT =>
              scanComment()
              setScannerState(SCANNER_STATE_PROLOG)
            case SCANNER_STATE_PI =>
              scanPI()
              setScannerState(SCANNER_STATE_PROLOG)
            case SCANNER_STATE_DOCTYPE =>
              if (fDisallowDoctype) {
                reportFatalError("DoctypeNotAllowed", null)
              }
              if (fSeenDoctypeDecl) {
                reportFatalError("AlreadySeenDoctype", null)
              }
              fSeenDoctypeDecl = true
              if (scanDoctypeDecl()) {
                setScannerState(SCANNER_STATE_DTD_INTERNAL_DECLS)
                setDispatcher(fDTDDispatcher)
                return true
              }
              if (fDoctypeSystemId ne null) {
                fIsEntityDeclaredVC = !fStandalone
                if ((fValidation || fLoadExternalDTD) &&
                  ((fValidationManager eq null) || !fValidationManager.isCachedDTD)) {
                  setScannerState(SCANNER_STATE_DTD_EXTERNAL)
                  setDispatcher(fDTDDispatcher)
                  return true
                }
              } else if (fExternalSubsetSource ne null) {
                fIsEntityDeclaredVC = !fStandalone
                if ((fValidation || fLoadExternalDTD) &&
                  ((fValidationManager eq null) || !fValidationManager.isCachedDTD)) {
                  fDTDScanner.setInputSource(fExternalSubsetSource)
                  fExternalSubsetSource = null
                  setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS)
                  setDispatcher(fDTDDispatcher)
                  return true
                }
              }
              fDTDScanner.setInputSource(null)
              setScannerState(SCANNER_STATE_PROLOG)
            case SCANNER_STATE_CONTENT =>
              reportFatalError("ContentIllegalInProlog", null)
              fEntityScanner.scanChar()
              // @ebruchez: unclear in original Java code if fall-through to next case was intended!
            case SCANNER_STATE_REFERENCE =>
              reportFatalError("ReferenceIllegalInProlog", null)
          }
        } while (complete || again)
        if (complete) {
          if (fEntityScanner.scanChar() != '<') {
            reportFatalError("RootElementRequired", null)
          }
          setScannerState(SCANNER_STATE_ROOT_ELEMENT)
          setDispatcher(fContentDispatcher)
        }
      } catch {
        case e: MalformedByteSequenceException =>
          fErrorReporter.reportError(e.getDomain, e.getKey, e.getArguments, XMLErrorReporter.SEVERITY_FATAL_ERROR,
            e)
          return false
// @ebruchez: not supported in Scala.js
//        case e: CharConversionException =>
//          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR,
//            e)
//          return false
        case e: EOFException =>
          reportFatalError("PrematureEOF", null)
          return false
      }
      true
    }
  }

  /**
   * Dispatcher to handle the internal and external DTD subsets.
   */
  protected class DTDDispatcher extends Dispatcher {

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this dispatcher is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if there is more to dispatch either from this
     *          or a another dispatcher.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     */
    def dispatch(complete: Boolean): Boolean = {
      fEntityManager.setEntityHandler(null)
      try {
        var again: Boolean = false
        do {
          again = false
          fScannerState match {
            case SCANNER_STATE_DTD_INTERNAL_DECLS =>
              val caseBreaks = new Breaks
              caseBreaks.breakable {
                val completeDTD = true
                val readExternalSubset = (fValidation || fLoadExternalDTD) &&
                  ((fValidationManager eq null) || !fValidationManager.isCachedDTD)
                val moreToScan = fDTDScanner.scanDTDInternalSubset(completeDTD, fStandalone, fHasExternalDTD && readExternalSubset)
                if (!moreToScan) {
                  if (!fEntityScanner.skipChar(']')) {
                    reportFatalError("EXPECTED_SQUARE_BRACKET_TO_CLOSE_INTERNAL_SUBSET", null)
                  }
                  fEntityScanner.skipSpaces()
                  if (!fEntityScanner.skipChar('>')) {
                    reportFatalError("DoctypedeclUnterminated", Array(fDoctypeName))
                  }
                  fMarkupDepth -= 1
                  if (fDoctypeSystemId ne null) {
                    fIsEntityDeclaredVC = !fStandalone
                    if (readExternalSubset) {
                      setScannerState(SCANNER_STATE_DTD_EXTERNAL)
                      caseBreaks.break()
                    }
                  } else if (fExternalSubsetSource ne null) {
                    fIsEntityDeclaredVC = !fStandalone
                    if (readExternalSubset) {
                      fDTDScanner.setInputSource(fExternalSubsetSource)
                      fExternalSubsetSource = null
                      setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS)
                      caseBreaks.break()
                    }
                  } else {
                    fIsEntityDeclaredVC = fEntityManager.hasPEReferences && !fStandalone
                  }
                  setScannerState(SCANNER_STATE_PROLOG)
                  setDispatcher(fPrologDispatcher)
                  fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this)
                  return true
                }
              }
            case SCANNER_STATE_DTD_EXTERNAL =>
              fDTDDescription.setValues(fDoctypePublicId, fDoctypeSystemId, null, null)
              fDTDDescription.setRootName(fDoctypeName)
              val xmlInputSource = fEntityManager.resolveEntity(fDTDDescription)
              fDTDScanner.setInputSource(xmlInputSource)
              setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS)
              again = true
            case SCANNER_STATE_DTD_EXTERNAL_DECLS =>
              val completeDTD = true
              val moreToScan = fDTDScanner.scanDTDExternalSubset(completeDTD)
              if (!moreToScan) {
                setScannerState(SCANNER_STATE_PROLOG)
                setDispatcher(fPrologDispatcher)
                fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this)
                return true
              }
            case _ =>
              throw new XNIException("DTDDispatcher#dispatch: scanner state=" + fScannerState +
                " (" +
                getScannerStateName(fScannerState) +
                ')')
          }
        } while (complete || again);
      } catch {
        case e: MalformedByteSequenceException =>
          fErrorReporter.reportError(e.getDomain, e.getKey, e.getArguments, XMLErrorReporter.SEVERITY_FATAL_ERROR,
            e)
          return false
// @ebruchez: not supported in Scala.js
//        case e: CharConversionException =>
//          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR,
//            e)
//          return false
        case e: EOFException =>
          reportFatalError("PrematureEOF", null)
          return false
      } finally {
        fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this)
      }
      true
    }
  }

  /**
   * Dispatcher to handle content scanning.
   */
  protected class ContentDispatcher extends FragmentContentDispatcher {

    /**
     * Scan for DOCTYPE hook. This method is a hook for subclasses
     * to add code to handle scanning for a the "DOCTYPE" string
     * after the string "<!" has been scanned.
     *
     * @return True if the "DOCTYPE" was scanned; false if "DOCTYPE"
     *          was not scanned.
     */
    override protected def scanForDoctypeHook(): Boolean = {
      if (fEntityScanner.skipString("DOCTYPE")) {
        setScannerState(SCANNER_STATE_DOCTYPE)
        return true
      }
      false
    }

    /**
     * Element depth iz zero. This methos is a hook for subclasses
     * to add code to handle when the element depth hits zero. When
     * scanning a document fragment, an element depth of zero is
     * normal. However, when scanning a full XML document, the
     * scanner must handle the trailing miscellanous section of
     * the document after the end of the document's root element.
     *
     * @return True if the caller should stop and return true which
     *          allows the scanner to switch to a new scanning
     *          dispatcher. A return value of false indicates that
     *          the content dispatcher should continue as normal.
     */
    override protected def elementDepthIsZeroHook(): Boolean = {
      setScannerState(SCANNER_STATE_TRAILING_MISC)
      setDispatcher(fTrailingMiscDispatcher)
      true
    }

    /**
     * Scan for root element hook. This method is a hook for
     * subclasses to add code that handles scanning for the root
     * element. When scanning a document fragment, there is no
     * "root" element. However, when scanning a full XML document,
     * the scanner must handle the root element specially.
     *
     * @return True if the caller should stop and return true which
     *          allows the scanner to switch to a new scanning
     *          dispatcher. A return value of false indicates that
     *          the content dispatcher should continue as normal.
     */
    override protected def scanRootElementHook(): Boolean = {
      if ((fExternalSubsetResolver ne null) && ! fSeenDoctypeDecl &&
        ! fDisallowDoctype && (fValidation || fLoadExternalDTD)) {
        scanStartElementName()
        resolveExternalSubsetAndRead()
        if (scanStartElementAfterName()) {
          setScannerState(SCANNER_STATE_TRAILING_MISC)
          setDispatcher(fTrailingMiscDispatcher)
          return true
        }
      } else if (scanStartElement()) {
        setScannerState(SCANNER_STATE_TRAILING_MISC)
        setDispatcher(fTrailingMiscDispatcher)
        return true
      }
      false
    }

    /**
     * End of file hook. This method is a hook for subclasses to
     * add code that handles the end of file. The end of file in
     * a document fragment is OK if the markup depth is zero.
     * However, when scanning a full XML document, an end of file
     * is always premature.
     */
    override protected def endOfFileHook(e: EOFException): Unit = {
      reportFatalError("PrematureEOF", null)
    }

    /**
     * Attempt to locate an external subset for a document that does not otherwise
     * have one. If an external subset is located, then it is scanned.
     */
    protected def resolveExternalSubsetAndRead(): Unit = {
      fDTDDescription.setValues(null, null, fEntityManager.getCurrentResourceIdentifier.getExpandedSystemId,
        null)
      fDTDDescription.setRootName(fElementQName.rawname)
      val src = fExternalSubsetResolver.getExternalSubset(fDTDDescription)
      if (src ne null) {
        fDoctypeName = fElementQName.rawname
        fDoctypePublicId = src.getPublicId
        fDoctypeSystemId = src.getSystemId
        if (fDocumentHandler ne null) {
          fDocumentHandler.doctypeDecl(fDoctypeName, fDoctypePublicId, fDoctypeSystemId, null)
        }
        try {
          if ((fValidationManager eq null) || !fValidationManager.isCachedDTD) {
            fDTDScanner.setInputSource(src)
            while (fDTDScanner.scanDTDExternalSubset(complete = true))()
          } else {
            fDTDScanner.setInputSource(null)
          }
        } finally {
          fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this)
        }
      }
    }
  }

  /**
   * Dispatcher to handle trailing miscellaneous section scanning.
   */
  protected class TrailingMiscDispatcher extends Dispatcher {

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this dispatcher is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if there is more to dispatch either from this
     *          or a another dispatcher.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     */
    def dispatch(complete: Boolean): Boolean = {
      try {
        var again: Boolean = false
        do {
          again = false
          fScannerState match {
            case SCANNER_STATE_TRAILING_MISC =>
              fEntityScanner.skipSpaces()
              if (fEntityScanner.skipChar('<')) {
                setScannerState(SCANNER_STATE_START_OF_MARKUP)
                again = true
              } else {
                setScannerState(SCANNER_STATE_CONTENT)
                again = true
              }
            case SCANNER_STATE_START_OF_MARKUP =>
              fMarkupDepth += 1
              if (fEntityScanner.skipChar('?')) {
                setScannerState(SCANNER_STATE_PI)
                again = true
              } else if (fEntityScanner.skipChar('!')) {
                setScannerState(SCANNER_STATE_COMMENT)
                again = true
              } else if (fEntityScanner.skipChar('/')) {
                reportFatalError("MarkupNotRecognizedInMisc", null)
                again = true
              } else if (isValidNameStartChar(fEntityScanner.peekChar())) {
                reportFatalError("MarkupNotRecognizedInMisc", null)
                scanStartElement()
                setScannerState(SCANNER_STATE_CONTENT)
              } else if (isValidNameStartHighSurrogate(fEntityScanner.peekChar())) {
                reportFatalError("MarkupNotRecognizedInMisc", null)
                scanStartElement()
                setScannerState(SCANNER_STATE_CONTENT)
              } else {
                reportFatalError("MarkupNotRecognizedInMisc", null)
              }
            case SCANNER_STATE_PI =>
              scanPI()
              setScannerState(SCANNER_STATE_TRAILING_MISC)
            case SCANNER_STATE_COMMENT =>
              if (!fEntityScanner.skipString("--")) {
                reportFatalError("InvalidCommentStart", null)
              }
              scanComment()
              setScannerState(SCANNER_STATE_TRAILING_MISC)
            case SCANNER_STATE_CONTENT =>
              val ch = fEntityScanner.peekChar()
              if (ch == -1) {
                setScannerState(SCANNER_STATE_TERMINATED)
                return false
              }
              println(s"xxx: ch = $ch")
              reportFatalError("ContentIllegalInTrailingMisc", null)
              fEntityScanner.scanChar()
              setScannerState(SCANNER_STATE_TRAILING_MISC)
            case SCANNER_STATE_REFERENCE =>
              reportFatalError("ReferenceIllegalInTrailingMisc", null)
              setScannerState(SCANNER_STATE_TRAILING_MISC)
            case SCANNER_STATE_TERMINATED =>
              return false
          }
        } while (complete || again);
      } catch {
        case e: MalformedByteSequenceException =>
          fErrorReporter.reportError(e.getDomain, e.getKey, e.getArguments, XMLErrorReporter.SEVERITY_FATAL_ERROR,
            e)
          return false
// @ebruchez: not supported in Scala.js
//        case e: CharConversionException =>
//          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR,
//            e)
//          return false
        case e: EOFException =>
          if (fMarkupDepth != 0) {
            reportFatalError("PrematureEOF", null)
            return false
          }
          setScannerState(SCANNER_STATE_TERMINATED)
          return false
      }
      true
    }
  }
}
