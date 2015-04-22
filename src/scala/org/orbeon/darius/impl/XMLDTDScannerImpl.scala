package org.orbeon.darius.impl

import java.io.IOException

import org.orbeon.darius.impl.XMLDTDScannerImpl._
import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.util.SymbolTable
import org.orbeon.darius.util.XMLChar
import org.orbeon.darius.util.XMLStringBuffer
import org.orbeon.darius.util.XMLSymbols
import org.orbeon.darius.xni.Augmentations
import org.orbeon.darius.xni.XMLDTDContentModelHandler
import org.orbeon.darius.xni.XMLDTDHandler
import org.orbeon.darius.xni.XMLResourceIdentifier
import org.orbeon.darius.xni.XMLString
import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.parser.XMLComponent
import org.orbeon.darius.xni.parser.XMLComponentManager
import org.orbeon.darius.xni.parser.XMLDTDScanner
import org.orbeon.darius.xni.parser.XMLInputSource
import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.util.SymbolTable
import org.orbeon.darius.util.XMLChar
import org.orbeon.darius.xni.Augmentations
import org.orbeon.darius.xni.XMLDTDContentModelHandler
import org.orbeon.darius.xni.XMLDTDHandler
import org.orbeon.darius.xni.XMLResourceIdentifier
import org.orbeon.darius.xni.XMLString
import org.orbeon.darius.xni.parser.XMLComponent
import org.orbeon.darius.xni.parser.XMLComponentManager
import org.orbeon.darius.xni.parser.XMLDTDScanner
import org.orbeon.darius.xni.parser.XMLInputSource

import scala.util.control.Breaks

protected[impl] object XMLDTDScannerImpl {
  
  import XMLScanner._

  /**
   Scanner state: end of input.
   */
  val SCANNER_STATE_END_OF_INPUT = 0

  /**
   Scanner state: text declaration.
   */
  val SCANNER_STATE_TEXT_DECL = 1

  /**
   Scanner state: markup declaration.
   */
  val SCANNER_STATE_MARKUP_DECL = 2

  /**
   Recognized features.
   */
  private val RECOGNIZED_FEATURES = Array(VALIDATION, NOTIFY_CHAR_REFS)

  /**
   Feature defaults.
   */
  private val FEATURE_DEFAULTS = Array[java.lang.Boolean](null, false)

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(SYMBOL_TABLE, ERROR_REPORTER, ENTITY_MANAGER)

  /**
   Property defaults.
   */
  private val PROPERTY_DEFAULTS = Array(null, null, null)

  /**
   Debug scanner state.
   */
  private val DEBUG_SCANNER_STATE = false

  /**
   Returns the scanner state name.
   */
  private def getScannerStateName(state: Int): String = {
    if (DEBUG_SCANNER_STATE) state match {
      case SCANNER_STATE_END_OF_INPUT ⇒ return "SCANNER_STATE_END_OF_INPUT"
      case SCANNER_STATE_TEXT_DECL    ⇒ return "SCANNER_STATE_TEXT_DECL"
      case SCANNER_STATE_MARKUP_DECL  ⇒ return "SCANNER_STATE_MARKUP_DECL"
    }
    "??? (" + state + ')'
  }
}

/**
 * This class is responsible for scanning the declarations found
 * in the internal and external subsets of a DTD in an XML document.
 * The scanner acts as the sources for the DTD information which is
 * communicated to the DTD handlers.
 * 
 * This component requires the following features and properties from the
 * component manager that uses it:
 * 
 *  - http://xml.org/sax/features/validation
 *  - http://apache.org/xml/features/scanner/notify-char-refs
 *  - http://apache.org/xml/properties/internal/symbol-table
 *  - http://apache.org/xml/properties/internal/error-reporter
 *  - http://apache.org/xml/properties/internal/entity-manager
 */
class XMLDTDScannerImpl extends XMLScanner with XMLDTDScanner with XMLComponent with XMLEntityHandler {

  import XMLScanner._
  
  /**
   DTD handler.
   */
  protected var fDTDHandler: XMLDTDHandler = _

  /**
   DTD content model handler.
   */
  protected var fDTDContentModelHandler: XMLDTDContentModelHandler = _

  /**
   Scanner state.
   */
  protected var fScannerState: Int = _

  /**
   Standalone.
   */
  protected var fStandalone: Boolean = _

  /**
   Seen external DTD.
   */
  protected var fSeenExternalDTD: Boolean = _

  /**
   Seen a parameter entity reference.
   */
  protected var fSeenPEReferences: Boolean = _

  /**
   Start DTD called.
   */
  private var fStartDTDCalled: Boolean = _

  /**
   * Stack of content operators (either '|' or ',') in children
   * content.
   */
  private var fContentStack: Array[Int] = new Array[Int](5)

  /**
   Size of content stack.
   */
  private var fContentDepth: Int = _

  /**
   Parameter entity stack to check well-formedness.
   */
  private var fPEStack: Array[Int] = new Array[Int](5)

  /**
   Parameter entity stack to report start/end entity calls.
   */
  private var fPEReport: Array[Boolean] = new Array[Boolean](5)

  /**
   Number of opened parameter entities.
   */
  private var fPEDepth: Int = _

  /**
   Markup depth.
   */
  private var fMarkUpDepth: Int = _

  /**
   Number of opened external entities.
   */
  private var fExtEntityDepth: Int = _

  /**
   Number of opened include sections.
   */
  private var fIncludeSectDepth: Int = _

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
   String buffer.
   */
  private val fStringBuffer2 = new XMLStringBuffer()

  /**
   Literal text.
   */
  private val fLiteral = new XMLString()

  /**
   Literal text.
   */
  private val fLiteral2 = new XMLString()

  /**
   Enumeration values.
   */
  private var fEnumeration: Array[String] = new Array[String](5)

  /**
   Enumeration values count.
   */
  private var fEnumerationCount: Int = _

  /**
   Ignore conditional section buffer.
   */
  private val fIgnoreConditionalBuffer = new XMLStringBuffer(128)

  /**
   Constructor for he use of non-XMLComponentManagers.
   */
  def this(symbolTable: SymbolTable, errorReporter: XMLErrorReporter, entityManager: XMLEntityManager) {
    this()
    fSymbolTable = symbolTable
    fErrorReporter = errorReporter
    fEntityManager = entityManager
    entityManager.setProperty(SYMBOL_TABLE, fSymbolTable)
  }

  /**
   * Sets the input source.
   *
   * @param inputSource The input source or null.
   *
   * @throws IOException Thrown on i/o error.
   */
  def setInputSource(inputSource: XMLInputSource): Unit = {
    if (inputSource eq null) {
      if (fDTDHandler ne null) {
        fDTDHandler.startDTD(null, null)
        fDTDHandler.endDTD(null)
      }
      return
    }
    fEntityManager.setEntityHandler(this)
    fEntityManager.startDTDEntity(inputSource)
  }

  /**
   * Scans the external subset of the document.
   *
   * @param complete True if the scanner should scan the document
   *                 completely, pushing all events to the registered
   *                 document handler. A value of false indicates that
   *                 that the scanner should only scan the next portion
   *                 of the document and return. A scanner instance is
   *                 permitted to completely scan a document if it does
   *                 not support this "pull" scanning model.
   *
   * @return True if there is more to scan, false otherwise.
   */
  def scanDTDExternalSubset(complete: Boolean): Boolean = {
    fEntityManager.setEntityHandler(this)
    if (fScannerState == SCANNER_STATE_TEXT_DECL) {
      fSeenExternalDTD = true
      val textDecl = scanTextDecl()
      if (fScannerState == SCANNER_STATE_END_OF_INPUT) {
        return false
      } else {
        setScannerState(SCANNER_STATE_MARKUP_DECL)
        if (textDecl && !complete) {
          return true
        }
      }
    }
    do {
      if (!scanDecls(complete)) {
        return false
      }
    } while (complete)
    true
  }

  /**
   * Scans the internal subset of the document.
   *
   * @param complete True if the scanner should scan the document
   *                 completely, pushing all events to the registered
   *                 document handler. A value of false indicates that
   *                 that the scanner should only scan the next portion
   *                 of the document and return. A scanner instance is
   *                 permitted to completely scan a document if it does
   *                 not support this "pull" scanning model.
   * @param standalone True if the document was specified as standalone.
   *                   This value is important for verifying certain
   *                   well-formedness constraints.
   * @param hasExternalSubset True if the document has an external DTD.
   *                          This allows the scanner to properly notify
   *                          the handler of the end of the DTD in the
   *                          absence of an external subset.
   *
   * @return True if there is more to scan, false otherwise.
   */
  def scanDTDInternalSubset(complete: Boolean, standalone: Boolean, hasExternalSubset: Boolean): Boolean = {
    fEntityScanner = fEntityManager.getEntityScanner
    fEntityManager.setEntityHandler(this)
    fStandalone = standalone
    if (fScannerState == SCANNER_STATE_TEXT_DECL) {
      if (fDTDHandler ne null) {
        fDTDHandler.startDTD(fEntityScanner, null)
        fStartDTDCalled = true
      }
      setScannerState(SCANNER_STATE_MARKUP_DECL)
    }
    do {
      if (!scanDecls(complete)) {
        if ((fDTDHandler ne null) && ! hasExternalSubset) {
          fDTDHandler.endDTD(null)
        }
        setScannerState(SCANNER_STATE_TEXT_DECL)
        return false
      }
    } while (complete)
    true
  }

  override def reset(componentManager: XMLComponentManager): Unit = {
    super.reset(componentManager)
    init()
  }

  override def reset(): Unit = {
    super.reset()
    init()
  }

  /**
   * Returns a list of feature identifiers that are recognized by
   * this component. This method may return null if no features
   * are recognized by this component.
   */
  def getRecognizedFeatures: Array[String] = {
    RECOGNIZED_FEATURES.clone()
  }

  /**
   * Returns a list of property identifiers that are recognized by
   * this component. This method may return null if no properties
   * are recognized by this component.
   */
  def getRecognizedProperties: Array[String] = {
    RECOGNIZED_PROPERTIES.clone()
  }

  /**
   * Returns the default state for a feature, or null if this
   * component does not want to report a default value for this
   * feature.
   *
   * @param featureId The feature identifier.
   *
   * @since Xerces 2.2.0
   */
  def getFeatureDefault(featureId: String): java.lang.Boolean = {
    RECOGNIZED_FEATURES.indices.find(RECOGNIZED_FEATURES(_) == featureId)
      .map(FEATURE_DEFAULTS(_)).orNull
  }

  /**
   * Returns the default state for a property, or null if this
   * component does not want to report a default value for this
   * property.
   *
   * @param propertyId The property identifier.
   *
   * @since Xerces 2.2.0
   */
  def getPropertyDefault(propertyId: String): AnyRef = {
    RECOGNIZED_PROPERTIES.indices.find(RECOGNIZED_PROPERTIES(_) == propertyId)
      .map(PROPERTY_DEFAULTS(_)).orNull
  }

  def setDTDHandler(dtdHandler: XMLDTDHandler): Unit = {
    fDTDHandler = dtdHandler
  }

  def getDTDHandler: XMLDTDHandler = fDTDHandler

  def setDTDContentModelHandler(dtdContentModelHandler: XMLDTDContentModelHandler): Unit = {
    fDTDContentModelHandler = dtdContentModelHandler
  }

  /**
   * getDTDContentModelHandler
   *
   * @return XMLDTDContentModelHandler
   */
  def getDTDContentModelHandler: XMLDTDContentModelHandler = fDTDContentModelHandler

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
   * @param augs     Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  override def startEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augs: Augmentations): Unit = {
    super.startEntity(name, identifier, encoding, augs)
    val dtdEntity = name == "[dtd]"
    if (dtdEntity) {
      if ((fDTDHandler ne null) && !fStartDTDCalled) {
        fDTDHandler.startDTD(fEntityScanner, null)
      }
      if (fDTDHandler ne null) {
        fDTDHandler.startExternalSubset(identifier, null)
      }
      fEntityManager.startExternalSubset()
      fExtEntityDepth += 1
    } else if (name.charAt(0) == '%') {
      pushPEStack(fMarkUpDepth, fReportEntity)
      if (fEntityScanner.isExternal) {
        fExtEntityDepth += 1
      }
    }
    if ((fDTDHandler ne null) && ! dtdEntity && fReportEntity) {
      fDTDHandler.startParameterEntity(name, identifier, encoding, augs)
    }
  }

  /**
   * This method notifies the end of an entity. The DTD has the pseudo-name
   * of "[dtd]" parameter entity names start with '%'; and general entities
   * are just specified by their name.
   *
   * @param name The name of the entity.
   * @param augs Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  override def endEntity(name: String, augs: Augmentations): Unit = {
    super.endEntity(name, augs)
    if (fScannerState == SCANNER_STATE_END_OF_INPUT) return
    var reportEntity = fReportEntity
    if (name.startsWith("%")) {
      reportEntity = peekReportEntity()
      val startMarkUpDepth = popPEStack()
      if (startMarkUpDepth == 0 && startMarkUpDepth < fMarkUpDepth) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "ILL_FORMED_PARAMETER_ENTITY_WHEN_USED_IN_DECL", 
          Array(fEntityManager.fCurrentEntity.name), XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
      if (startMarkUpDepth != fMarkUpDepth) {
        reportEntity = false
        if (fValidation) {
          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "ImproperDeclarationNesting", Array(name), 
            XMLErrorReporter.SEVERITY_ERROR)
        }
      }
      if (fEntityScanner.isExternal) {
        fExtEntityDepth -= 1
      }
      if ((fDTDHandler ne null) && reportEntity) {
        fDTDHandler.endParameterEntity(name, augs)
      }
    } else if (name == "[dtd]") {
      if (fIncludeSectDepth != 0) {
        reportFatalError("IncludeSectUnterminated", null)
      }
      fScannerState = SCANNER_STATE_END_OF_INPUT
      fEntityManager.endExternalSubset()
      if (fDTDHandler ne null) {
        fDTDHandler.endExternalSubset(null)
        fDTDHandler.endDTD(null)
      }
      fExtEntityDepth -= 1
    }
  }

  /**
   * Sets the scanner state.
   *
   * @param state The new scanner state.
   */
  protected def setScannerState(state: Int): Unit = {
    fScannerState = state
    if (DEBUG_SCANNER_STATE) {
      System.out.print("### setScannerState: ")
      System.out.print(getScannerStateName(state))
      println()
    }
  }

  protected def scanningInternalSubset(): Boolean = fExtEntityDepth == 0

  /**
   * start a parameter entity dealing with the textdecl if there is any
   *
   * @param name The name of the parameter entity to start (without the '%')
   * @param literal Whether this is happening within a literal
   */
  protected def startPE(name: String, literal: Boolean): Unit = {
    val depth = fPEDepth
    val pName = "%" + name
    if (!fSeenPEReferences) {
      fSeenPEReferences = true
      fEntityManager.notifyHasPEReferences()
    }
    if (fValidation && !fEntityManager.isDeclaredEntity(pName)) {
      fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EntityNotDeclared", Array(name), XMLErrorReporter.SEVERITY_ERROR)
    }
    fEntityManager.startEntity(fSymbolTable.addSymbol(pName), literal)
    if (depth != fPEDepth && fEntityScanner.isExternal) {
      scanTextDecl()
    }
  }

  /**
   * Dispatch an XML "event".
   *
   * @return true if a TextDecl was scanned.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown on parse error.
   *
   */
  protected def scanTextDecl(): Boolean = {
    var textDecl = false
    if (fEntityScanner.skipString("<?xml")) {
      fMarkUpDepth += 1
      if (isValidNameChar(fEntityScanner.peekChar())) {
        fStringBuffer.clear()
        fStringBuffer.append("xml")
        if (fNamespaces) {
          while (isValidNCName(fEntityScanner.peekChar())) {
            fStringBuffer.append(fEntityScanner.scanChar().toChar)
          }
        } else {
          while (isValidNameChar(fEntityScanner.peekChar())) {
            fStringBuffer.append(fEntityScanner.scanChar().toChar)
          }
        }
        val target = fSymbolTable.addSymbol(fStringBuffer.ch, fStringBuffer.offset, fStringBuffer.length)
        scanPIData(target, fString)
      } else {
        var version: String = null
        var encoding: String = null
        scanXMLDeclOrTextDecl(scanningTextDecl = true, fStrings)
        textDecl = true
        fMarkUpDepth -= 1
        version = fStrings(0)
        encoding = fStrings(1)
        fEntityScanner.setXMLVersion(version)
        if (!fEntityScanner.fCurrentEntity.isEncodingExternallySpecified) {
          fEntityScanner.setEncoding(encoding)
        }
        if (fDTDHandler ne null) {
          fDTDHandler.textDecl(version, encoding, null)
        }
      }
    }
    fEntityManager.fCurrentEntity.mayReadChunks = true
    textDecl
  }

  /**
   * Scans a processing data. This is needed to handle the situation
   * where a document starts with a processing instruction whose
   * target name *starts with* "xml". (e.g. xmlfoo)
   *
   * @param target The PI target
   * @param data The string to fill in with the data
   */
  override protected def scanPIData(target: String, data: XMLString): Unit = {
    super.scanPIData(target, data)
    fMarkUpDepth -= 1
    if (fDTDHandler ne null) {
      fDTDHandler.processingInstruction(target, data, null)
    }
  }

  /**
   * Scans a comment.
   * 
   *  [15] Comment ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
   * 
   * *Note:* Called after scanning past '<!--'
   */
  protected def scanComment(): Unit = {
    fReportEntity = false
    scanComment(fStringBuffer)
    fMarkUpDepth -= 1
    if (fDTDHandler ne null) {
      fDTDHandler.comment(fStringBuffer, null)
    }
    fReportEntity = true
  }

  /**
   * Scans an element declaration
   * 
   *  [45]    elementdecl    ::=    '<!ELEMENT' S Name S contentspec S? '>'
   *  [46]    contentspec    ::=    'EMPTY' | 'ANY' | Mixed | children
   * 
   * *Note:* Called after scanning past '<!ELEMENT'
   */
  protected def scanElementDecl(): Unit = {
    fReportEntity = false
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL", null)
    }
    val name = fEntityScanner.scanName()
    if (name eq null) {
      reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL", null)
    }
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL", Array(name))
    }
    if (fDTDContentModelHandler ne null) {
      fDTDContentModelHandler.startContentModel(name, null)
    }
    var contentModel: String = null
    fReportEntity = true
    if (fEntityScanner.skipString("EMPTY")) {
      contentModel = "EMPTY"
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.empty(null)
      }
    } else if (fEntityScanner.skipString("ANY")) {
      contentModel = "ANY"
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.any(null)
      }
    } else {
      if (!fEntityScanner.skipChar('(')) {
        reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN", Array(name))
      }
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.startGroup(null)
      }
      fStringBuffer.clear()
      fStringBuffer.append('(')
      fMarkUpDepth += 1
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
      if (fEntityScanner.skipString("#PCDATA")) {
        scanMixed(name)
      } else {
        scanChildren(name)
      }
      contentModel = fStringBuffer.toString
    }
    if (fDTDContentModelHandler ne null) {
      fDTDContentModelHandler.endContentModel(null)
    }
    fReportEntity = false
    skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("ElementDeclUnterminated", Array(name))
    }
    fReportEntity = true
    fMarkUpDepth -= 1
    if (fDTDHandler ne null) {
      fDTDHandler.elementDecl(name, contentModel, null)
    }
  }

  /**
   * scan Mixed content model
   * This assumes the content model has been parsed up to #PCDATA and
   * can simply append to fStringBuffer.
   * 
   *  [51]    Mixed    ::=    '(' S? '#PCDATA' (S? '|' S? Name)* S? ')*'
   *                        | '(' S? '#PCDATA' S? ')'
   *
   * @param elName The element type name this declaration is about.
   *
   * *Note:* Called after scanning past '(#PCDATA'.
   */
  private def scanMixed(elName: String): Unit = {
    var childName: String = null
    fStringBuffer.append("#PCDATA")
    if (fDTDContentModelHandler ne null) {
      fDTDContentModelHandler.pcdata(null)
    }
    skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    while (fEntityScanner.skipChar('|')) {
      fStringBuffer.append('|')
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_CHOICE, null)
      }
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
      childName = fEntityScanner.scanName()
      if (childName eq null) {
        reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT", Array(elName))
      }
      fStringBuffer.append(childName)
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.element(childName, null)
      }
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    }
    if (fEntityScanner.skipString(")*")) {
      fStringBuffer.append(")*")
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.endGroup(null)
        fDTDContentModelHandler.occurrence(XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE, null)
      }
    } else if (childName ne null) {
      reportFatalError("MixedContentUnterminated", Array(elName))
    } else if (fEntityScanner.skipChar(')')) {
      fStringBuffer.append(')')
      if (fDTDContentModelHandler ne null) {
        fDTDContentModelHandler.endGroup(null)
      }
    } else {
      reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN", Array(elName))
    }
    fMarkUpDepth -= 1
  }

  /**
   * scan children content model
   * This assumes it can simply append to fStringBuffer.
   * 
   *  [47]    children  ::=    (choice | seq) ('?' | '*' | '+')?
   *  [48]    cp        ::=    (Name | choice | seq) ('?' | '*' | '+')?
   *  [49]    choice    ::=    '(' S? cp ( S? '|' S? cp )+ S? ')'
   *  [50]    seq       ::=    '(' S? cp ( S? ',' S? cp )* S? ')'
   *
   * @param elName The element type name this declaration is about.
   *
   * *Note:* Called after scanning past the first open
   * paranthesis.
   */
  private def scanChildren(elName: String): Unit = {
    fContentDepth = 0
    pushContentStack(0)
    var currentOp = 0
    var c: Int = 0
    while (true) {
      val continueBreaks = new Breaks
      continueBreaks.breakable {
        if (fEntityScanner.skipChar('(')) {
          fMarkUpDepth += 1
          fStringBuffer.append('(')
          if (fDTDContentModelHandler ne null) {
            fDTDContentModelHandler.startGroup(null)
          }
          pushContentStack(currentOp)
          currentOp = 0
          skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
          continueBreaks.break()
        }
        skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
        val childName = fEntityScanner.scanName()
        if (childName eq null) {
          reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN", Array(elName))
          return
        }
        if (fDTDContentModelHandler ne null) {
          fDTDContentModelHandler.element(childName, null)
        }
        fStringBuffer.append(childName)
        c = fEntityScanner.peekChar()
        if (c == '?' || c == '*' || c == '+') {
          if (fDTDContentModelHandler ne null) {
            var oc: Short = 0
            oc = if (c == '?') XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE else if (c == '*') XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE else XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE
            fDTDContentModelHandler.occurrence(oc, null)
          }
          fEntityScanner.scanChar()
          fStringBuffer.append(c.toChar)
        }
        val whileBreaks = new Breaks
        whileBreaks.breakable {
          while (true) {
            skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
            c = fEntityScanner.peekChar()
            if (c == ',' && currentOp != '|') {
              currentOp = c
              if (fDTDContentModelHandler ne null) {
                fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_SEQUENCE, null)
              }
              fEntityScanner.scanChar()
              fStringBuffer.append(',')
              whileBreaks.break()
            } else if (c == '|' && currentOp != ',') {
              currentOp = c
              if (fDTDContentModelHandler ne null) {
                fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_CHOICE, null)
              }
              fEntityScanner.scanChar()
              fStringBuffer.append('|')
              whileBreaks.break()
            } else if (c != ')') {
              reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN", Array(elName))
            }
            if (fDTDContentModelHandler ne null) {
              fDTDContentModelHandler.endGroup(null)
            }
            currentOp = popContentStack()
            var oc: Short = 0
            if (fEntityScanner.skipString(")?")) {
              fStringBuffer.append(")?")
              if (fDTDContentModelHandler ne null) {
                oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE
                fDTDContentModelHandler.occurrence(oc, null)
              }
            } else if (fEntityScanner.skipString(")+")) {
              fStringBuffer.append(")+")
              if (fDTDContentModelHandler ne null) {
                oc = XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE
                fDTDContentModelHandler.occurrence(oc, null)
              }
            } else if (fEntityScanner.skipString(")*")) {
              fStringBuffer.append(")*")
              if (fDTDContentModelHandler ne null) {
                oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE
                fDTDContentModelHandler.occurrence(oc, null)
              }
            } else {
              fEntityScanner.scanChar()
              fStringBuffer.append(')')
            }
            fMarkUpDepth -= 1
            if (fContentDepth == 0) {
              return
            }
          }
        }
        skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
      }
    }
  }

  /**
   * Scans an attlist declaration
   * 
   *  [52]  AttlistDecl    ::=   '<!ATTLIST' S Name AttDef* S? '>'
   *  [53]  AttDef         ::=   S Name S AttType S DefaultDecl
   * 
   * *Note:* Called after scanning past '<!ATTLIST'
   */
  protected def scanAttlistDecl(): Unit = {
    fReportEntity = false
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL", null)
    }
    val elName = fEntityScanner.scanName()
    if (elName eq null) {
      reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL", null)
    }
    if (fDTDHandler ne null) {
      fDTDHandler.startAttlist(elName, null)
    }
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      if (fEntityScanner.skipChar('>')) {
        if (fDTDHandler ne null) {
          fDTDHandler.endAttlist(null)
        }
        fMarkUpDepth -= 1
        return
      } else {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF", Array(elName))
      }
    }
    while (!fEntityScanner.skipChar('>')) {
      val name = fEntityScanner.scanName()
      if (name eq null) {
        reportFatalError("AttNameRequiredInAttDef", Array(elName))
      }
      if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF", Array(elName, name))
      }
      val `type` = scanAttType(elName, name)
      if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF", Array(elName, name))
      }
      val defaultType = scanAttDefaultDecl(elName, name, `type`, fLiteral, fLiteral2)
      if (fDTDHandler ne null) {
        var enumeration: Array[String] = null
        if (fEnumerationCount != 0) {
          enumeration = new Array[String](fEnumerationCount)
          System.arraycopy(fEnumeration, 0, enumeration, 0, fEnumerationCount)
        }
        if ((defaultType ne null) && (defaultType == "#REQUIRED" || defaultType == "#IMPLIED")) {
          fDTDHandler.attributeDecl(elName, name, `type`, enumeration, defaultType, null, null, null)
        } else {
          fDTDHandler.attributeDecl(elName, name, `type`, enumeration, defaultType, fLiteral, fLiteral2, 
            null)
        }
      }
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    }
    if (fDTDHandler ne null) {
      fDTDHandler.endAttlist(null)
    }
    fMarkUpDepth -= 1
    fReportEntity = true
  }

  /**
   * Scans an attribute type definition
   * 
   *  [54]  AttType        ::=   StringType | TokenizedType | EnumeratedType
   *  [55]  StringType     ::=   'CDATA'
   *  [56]  TokenizedType  ::=   'ID'
   *                           | 'IDREF'
   *                           | 'IDREFS'
   *                           | 'ENTITY'
   *                           | 'ENTITIES'
   *                           | 'NMTOKEN'
   *                           | 'NMTOKENS'
   *  [57]  EnumeratedType ::=    NotationType | Enumeration
   *  [58]  NotationType ::= 'NOTATION' S '(' S? Name (S? '|' S? Name)* S? ')'
   *  [59]  Enumeration    ::=    '(' S? Nmtoken (S? '|' S? Nmtoken)* S? ')'
   * 
   * *Note:* Called after scanning past '<!ATTLIST'
   *
   * @param elName The element type name this declaration is about.
   * @param atName The attribute name this declaration is about.
   */
  private def scanAttType(elName: String, atName: String): String = {
    var `type`: String = null
    fEnumerationCount = 0
    if (fEntityScanner.skipString("CDATA")) {
      `type` = "CDATA"
    } else if (fEntityScanner.skipString("IDREFS")) {
      `type` = "IDREFS"
    } else if (fEntityScanner.skipString("IDREF")) {
      `type` = "IDREF"
    } else if (fEntityScanner.skipString("ID")) {
      `type` = "ID"
    } else if (fEntityScanner.skipString("ENTITY")) {
      `type` = "ENTITY"
    } else if (fEntityScanner.skipString("ENTITIES")) {
      `type` = "ENTITIES"
    } else if (fEntityScanner.skipString("NMTOKENS")) {
      `type` = "NMTOKENS"
    } else if (fEntityScanner.skipString("NMTOKEN")) {
      `type` = "NMTOKEN"
    } else if (fEntityScanner.skipString("NOTATION")) {
      `type` = "NOTATION"
      if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
        reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE", Array(elName, atName))
      }
      var c = fEntityScanner.scanChar()
      if (c != '(') {
        reportFatalError("MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE", Array(elName, atName))
      }
      fMarkUpDepth += 1
      val doBreaks = new Breaks
      doBreaks.breakable {
        do {
          skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
          val aName = fEntityScanner.scanName()
          if (aName eq null) {
            reportFatalError("MSG_NAME_REQUIRED_IN_NOTATIONTYPE", Array(elName, atName))
            c = skipInvalidEnumerationValue()
            if (c != '|')
              doBreaks.break()
          } else {
            ensureEnumerationSize(fEnumerationCount + 1)
            fEnumeration(fEnumerationCount) = aName
            fEnumerationCount += 1
            skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
            c = fEntityScanner.scanChar()
          }
        } while (c == '|')
      }
      if (c != ')') {
        reportFatalError("NotationTypeUnterminated", Array(elName, atName))
      }
      fMarkUpDepth -= 1
    } else {
      `type` = "ENUMERATION"
      var c = fEntityScanner.scanChar()
      if (c != '(') {
        reportFatalError("AttTypeRequiredInAttDef", Array(elName, atName))
      }
      fMarkUpDepth += 1
      val doBreaks = new Breaks
      doBreaks.breakable {
        do {
          skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
          val token = fEntityScanner.scanNmtoken()
          if (token eq null) {
            reportFatalError("MSG_NMTOKEN_REQUIRED_IN_ENUMERATION", Array(elName, atName))
            c = skipInvalidEnumerationValue()
            if (c != '|')
              doBreaks.break()
          } else {
            ensureEnumerationSize(fEnumerationCount + 1)
            fEnumeration(fEnumerationCount) = token
            fEnumerationCount += 1
            skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
            c = fEntityScanner.scanChar()
          }
        } while (c == '|')
      }
      if (c != ')') {
        reportFatalError("EnumerationUnterminated", Array(elName, atName))
      }
      fMarkUpDepth -= 1
    }
    `type`
  }

  /**
   * Scans an attribute default declaration
   * 
   *  [60] DefaultDecl ::= '#REQUIRED' | '#IMPLIED' | (('#FIXED' S)? AttValue)
   *
   * @param atName The name of the attribute being scanned.
   * @param defaultVal The string to fill in with the default value.
   */
  protected def scanAttDefaultDecl(elName: String, 
      atName                  : String, 
      `type`                  : String, 
      defaultVal              : XMLString, 
      nonNormalizedDefaultVal : XMLString
  ): String = {
    var defaultType: String = null
    fString.clear()
    defaultVal.clear()
    if (fEntityScanner.skipString("#REQUIRED")) {
      defaultType = "#REQUIRED"
    } else if (fEntityScanner.skipString("#IMPLIED")) {
      defaultType = "#IMPLIED"
    } else {
      if (fEntityScanner.skipString("#FIXED")) {
        defaultType = "#FIXED"
        if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
          reportFatalError("MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL", Array(elName, atName))
        }
      }
      val isVC = !fStandalone && (fSeenExternalDTD || fSeenPEReferences)
      scanAttributeValue(defaultVal, nonNormalizedDefaultVal, atName, isVC, elName)
    }
    defaultType
  }

  /**
   * Scans an entity declaration
   * 
   *  [70]    EntityDecl  ::=    GEDecl | PEDecl
   *  [71]    GEDecl      ::=    '<!ENTITY' S Name S EntityDef S? '>'
   *  [72]    PEDecl      ::=    '<!ENTITY' S '%' S Name S PEDef S? '>'
   *  [73]    EntityDef   ::=    EntityValue | (ExternalID NDataDecl?)
   *  [74]    PEDef       ::=    EntityValue | ExternalID
   *  [75]    ExternalID  ::=    'SYSTEM' S SystemLiteral
   *                           | 'PUBLIC' S PubidLiteral S SystemLiteral
   *  [76]    NDataDecl   ::=    S 'NDATA' S Name
   * 
   * *Note:* Called after scanning past '<!ENTITY'
   */
  private def scanEntityDecl(): Unit = {
    var isPEDecl = false
    var sawPERef = false
    fReportEntity = false
    if (fEntityScanner.skipSpaces()) {
      if (!fEntityScanner.skipChar('%')) {
        isPEDecl = false
      } else if (skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
        isPEDecl = true
      } else if (scanningInternalSubset()) {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL", null)
        isPEDecl = true
      } else if (fEntityScanner.peekChar() == '%') {
        skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
        isPEDecl = true
      } else {
        sawPERef = true
      }
    } else if (scanningInternalSubset() || !fEntityScanner.skipChar('%')) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL", null)
      isPEDecl = false
    } else if (fEntityScanner.skipSpaces()) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL", null)
      isPEDecl = false
    } else {
      sawPERef = true
    }
    if (sawPERef) {
      val whileBreaks = new Breaks
      whileBreaks.breakable {
        while (true) {
          val peName = fEntityScanner.scanName()
          if (peName eq null) {
            reportFatalError("NameRequiredInPEReference", null)
          } else if (!fEntityScanner.skipChar(';')) {
            reportFatalError("SemicolonRequiredInPEReference", Array(peName))
          } else {
            startPE(peName, literal = false)
          }
          fEntityScanner.skipSpaces()
          if (!fEntityScanner.skipChar('%'))
            whileBreaks.break()
          if (!isPEDecl) {
            if (skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
              isPEDecl = true
              whileBreaks.break()
            }
            isPEDecl = fEntityScanner.skipChar('%')
          }
        }
      }
    }
    var name: String = null
    name = if (fNamespaces) fEntityScanner.scanNCName() else fEntityScanner.scanName()
    if (name eq null) {
      reportFatalError("MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL", null)
    }
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      if (fNamespaces && fEntityScanner.peekChar() == ':') {
        fEntityScanner.scanChar()
        val colonName = new XMLStringBuffer(name)
        colonName.append(':')
        val str = fEntityScanner.scanName()
        if (str ne null) colonName.append(str)
        reportFatalError("ColonNotLegalWithNS", Array(colonName.toString))
        if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
          reportFatalError("MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL", Array(name))
        }
      } else {
        reportFatalError("MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL", Array(name))
      }
    }
    scanExternalID(fStrings, optionalSystemId = false)
    val systemId = fStrings(0)
    val publicId = fStrings(1)
    var notation: String = null
    val sawSpace = skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())
    if (!isPEDecl && fEntityScanner.skipString("NDATA")) {
      if (!sawSpace) {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL", Array(name))
      }
      if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
        reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL", Array(name))
      }
      notation = fEntityScanner.scanName()
      if (notation eq null) {
        reportFatalError("MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL", Array(name))
      }
    }
    if (systemId eq null) {
      scanEntityValue(fLiteral, fLiteral2)
      fStringBuffer.clear()
      fStringBuffer2.clear()
      fStringBuffer.append(fLiteral.ch, fLiteral.offset, fLiteral.length)
      fStringBuffer2.append(fLiteral2.ch, fLiteral2.offset, fLiteral2.length)
    }
    skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("EntityDeclUnterminated", Array(name))
    }
    fMarkUpDepth -= 1
    if (isPEDecl) {
      name = "%" + name
    }
    if (systemId ne null) {
      val baseSystemId = fEntityScanner.getBaseSystemId
      if (notation ne null) {
        fEntityManager.addUnparsedEntity(name, publicId, systemId, baseSystemId, notation)
      } else {
        fEntityManager.addExternalEntity(name, publicId, systemId, baseSystemId)
      }
      if (fDTDHandler ne null) {
        fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, 
          baseSystemId, strict = false))
        if (notation ne null) {
          fDTDHandler.unparsedEntityDecl(name, fResourceIdentifier, notation, null)
        } else {
          fDTDHandler.externalEntityDecl(name, fResourceIdentifier, null)
        }
      }
    } else {
      fEntityManager.addInternalEntity(name, fStringBuffer.toString)
      if (fDTDHandler ne null) {
        fDTDHandler.internalEntityDecl(name, fStringBuffer, fStringBuffer2, null)
      }
    }
    fReportEntity = true
  }

  /**
   * Scans an entity value.
   *
   * @param value The string to fill in with the value.
   * @param nonNormalizedValue The string to fill in with the
   *                           non-normalized value.
   *
   * *Note:* This method uses fString, fStringBuffer (through
   * the use of scanCharReferenceValue), and fStringBuffer2, anything in them
   * at the time of calling is lost.
   */
  protected def scanEntityValue(value: XMLString, nonNormalizedValue: XMLString): Unit = {
    val quote = fEntityScanner.scanChar()
    if (quote != '\'' && quote != '"') {
      reportFatalError("OpenQuoteMissingInDecl", null)
    }
    val entityDepth = fEntityDepth
    var literal = fString
    var literal2 = fString
    if (fEntityScanner.scanLiteral(quote, fString) != quote) {
      fStringBuffer.clear()
      fStringBuffer2.clear()
      do {
        fStringBuffer.append(fString)
        fStringBuffer2.append(fString)
        if (fEntityScanner.skipChar('&')) {
          if (fEntityScanner.skipChar('#')) {
            fStringBuffer2.append("&#")
            scanCharReferenceValue(fStringBuffer, fStringBuffer2)
          } else {
            fStringBuffer.append('&')
            fStringBuffer2.append('&')
            val eName = fEntityScanner.scanName()
            if (eName eq null) {
              reportFatalError("NameRequiredInReference", null)
            } else {
              fStringBuffer.append(eName)
              fStringBuffer2.append(eName)
            }
            if (!fEntityScanner.skipChar(';')) {
              reportFatalError("SemicolonRequiredInReference", Array(eName))
            } else {
              fStringBuffer.append(';')
              fStringBuffer2.append(';')
            }
          }
        } else if (fEntityScanner.skipChar('%')) {
          import scala.util.control.Breaks._
          breakable {
            while (true) {
              fStringBuffer2.append('%')
              val peName = fEntityScanner.scanName()
              if (peName eq null) {
                reportFatalError("NameRequiredInPEReference", null)
              } else if (!fEntityScanner.skipChar(';')) {
                reportFatalError("SemicolonRequiredInPEReference", Array(peName))
              } else {
                if (scanningInternalSubset()) {
                  reportFatalError("PEReferenceWithinMarkup", Array(peName))
                }
                fStringBuffer2.append(peName)
                fStringBuffer2.append(';')
              }
              startPE(peName, literal = true)
              fEntityScanner.skipSpaces()
              if (!fEntityScanner.skipChar('%'))
                break()
            }
          }
        } else {
          val c = fEntityScanner.peekChar()
          if (XMLChar.isHighSurrogate(c)) {
            scanSurrogates(fStringBuffer2)
          } else if (isInvalidLiteral(c)) {
            reportFatalError("InvalidCharInLiteral", Array(Integer.toHexString(c)))
            fEntityScanner.scanChar()
          } else if (c != quote || entityDepth != fEntityDepth) {
            fStringBuffer.append(c.toChar)
            fStringBuffer2.append(c.toChar)
            fEntityScanner.scanChar()
          }
        }
      } while (fEntityScanner.scanLiteral(quote, fString) != quote)
      fStringBuffer.append(fString)
      fStringBuffer2.append(fString)
      literal = fStringBuffer
      literal2 = fStringBuffer2
    }
    value.setValues(literal)
    nonNormalizedValue.setValues(literal2)
    if (!fEntityScanner.skipChar(quote)) {
      reportFatalError("CloseQuoteMissingInDecl", null)
    }
  }

  /**
   * Scans a notation declaration
   * 
   *  [82] NotationDecl ::= '<!NOTATION' S Name S (ExternalID|PublicID) S? '>'
   *  [83]  PublicID    ::= 'PUBLIC' S PubidLiteral
   * 
   * *Note:* Called after scanning past '<!NOTATION'
   */
  private def scanNotationDecl(): Unit = {
    fReportEntity = false
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL", null)
    }
    var name: String = null
    name = if (fNamespaces) fEntityScanner.scanNCName() else fEntityScanner.scanName()
    if (name eq null) {
      reportFatalError("MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL", null)
    }
    if (!skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())) {
      if (fNamespaces && fEntityScanner.peekChar() == ':') {
        fEntityScanner.scanChar()
        val colonName = new XMLStringBuffer(name)
        colonName.append(':')
        colonName.append(fEntityScanner.scanName())
        reportFatalError("ColonNotLegalWithNS", Array(colonName.toString))
        skipSeparator(spaceRequired = true, lookForPERefs = !scanningInternalSubset())
      } else {
        reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL", Array(name))
      }
    }
    scanExternalID(fStrings, optionalSystemId = true)
    val systemId = fStrings(0)
    val publicId = fStrings(1)
    val baseSystemId = fEntityScanner.getBaseSystemId
    if ((systemId eq null) && (publicId eq null)) {
      reportFatalError("ExternalIDorPublicIDRequired", Array(name))
    }
    skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("NotationDeclUnterminated", Array(name))
    }
    fMarkUpDepth -= 1
    if (fDTDHandler ne null) {
      fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, 
        baseSystemId, strict = false))
      fDTDHandler.notationDecl(name, fResourceIdentifier, null)
    }
    fReportEntity = true
  }

  /**
   * Scans a conditional section. If it's a section to ignore the whole
   * section gets scanned through and this method only returns after the
   * closing bracket has been found. When it's an include section though, it
   * returns to let the main loop take care of scanning it. In that case the
   * end of the section if handled by the main loop (scanDecls).
   * 
   *  [61] conditionalSect   ::= includeSect | ignoreSect
   *  [62] includeSect       ::= '<![' S? 'INCLUDE' S? '[' extSubsetDecl ']]>'
   *  [63] ignoreSect   ::= '<![' S? 'IGNORE' S? '[' ignoreSectContents* ']]>'
   *  [64] ignoreSectContents ::= Ignore ('<![' ignoreSectContents ']]>' Ignore)*
   *  [65] Ignore            ::=    Char* - (Char* ('<![' | ']]>') Char*)
   * 
   * *Note:* Called after scanning past '<!['
   */
  private def scanConditionalSect(currPEDepth: Int): Unit = {
    fReportEntity = false
    skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
    if (fEntityScanner.skipString("INCLUDE")) {
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
      if (currPEDepth != fPEDepth && fValidation) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "INVALID_PE_IN_CONDITIONAL", Array(fEntityManager.fCurrentEntity.name), 
          XMLErrorReporter.SEVERITY_ERROR)
      }
      if (!fEntityScanner.skipChar('[')) {
        reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
      }
      if (fDTDHandler ne null) {
        fDTDHandler.startConditional(XMLDTDHandler.CONDITIONAL_INCLUDE, null)
      }
      fIncludeSectDepth += 1
      fReportEntity = true
    } else if (fEntityScanner.skipString("IGNORE")) {
      skipSeparator(spaceRequired = false, lookForPERefs = !scanningInternalSubset())
      if (currPEDepth != fPEDepth && fValidation) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "INVALID_PE_IN_CONDITIONAL", Array(fEntityManager.fCurrentEntity.name), 
          XMLErrorReporter.SEVERITY_ERROR)
      }
      if (fDTDHandler ne null) {
        fDTDHandler.startConditional(XMLDTDHandler.CONDITIONAL_IGNORE, null)
      }
      if (!fEntityScanner.skipChar('[')) {
        reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
      }
      fReportEntity = true
      fIncludeSectDepth += 1
      val initialDepth = fIncludeSectDepth
      if (fDTDHandler ne null) {
        fIgnoreConditionalBuffer.clear()
      }
      while (true) {
        if (fEntityScanner.skipChar('<')) {
          if (fDTDHandler ne null) {
            fIgnoreConditionalBuffer.append('<')
          }
          if (fEntityScanner.skipChar('!')) {
            if (fEntityScanner.skipChar('[')) {
              if (fDTDHandler ne null) {
                fIgnoreConditionalBuffer.append("![")
              }
              fIncludeSectDepth += 1
            } else {
              if (fDTDHandler ne null) {
                fIgnoreConditionalBuffer.append("!")
              }
            }
          }
        } else if (fEntityScanner.skipChar(']')) {
          if (fDTDHandler ne null) {
            fIgnoreConditionalBuffer.append(']')
          }
          if (fEntityScanner.skipChar(']')) {
            if (fDTDHandler ne null) {
              fIgnoreConditionalBuffer.append(']')
            }
            while (fEntityScanner.skipChar(']')) {
              if (fDTDHandler ne null) {
                fIgnoreConditionalBuffer.append(']')
              }
            }
            if (fEntityScanner.skipChar('>')) {
              fIncludeSectDepth -= 1
              if ((fIncludeSectDepth + 1) == initialDepth) {
                fMarkUpDepth -= 1
                if (fDTDHandler ne null) {
                  fLiteral.setValues(fIgnoreConditionalBuffer.ch, 0, fIgnoreConditionalBuffer.length - 2)
                  fDTDHandler.ignoredCharacters(fLiteral, null)
                  fDTDHandler.endConditional(null)
                }
                return
              } else if (fDTDHandler ne null) {
                fIgnoreConditionalBuffer.append('>')
              }
            }
          }
        } else {
          val c = fEntityScanner.scanChar()
          if (fScannerState == SCANNER_STATE_END_OF_INPUT) {
            reportFatalError("IgnoreSectUnterminated", null)
            return
          }
          if (fDTDHandler ne null) {
            fIgnoreConditionalBuffer.append(c.toChar)
          }
        }
      }
    } else {
      reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
    }
  }

  /**
   * Dispatch an XML "event".
   *
   * @param complete True if this method is intended to scan
   *                 and dispatch as much as possible.
   *
   * @return True if there is more to scan.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown on parse error.
   *
   */
  protected def scanDecls(complete: Boolean): Boolean = {
    skipSeparator(spaceRequired = false, lookForPERefs = true)
    var again = true
    while (again && fScannerState == SCANNER_STATE_MARKUP_DECL) {
      again = complete
      if (fEntityScanner.skipChar('<')) {
        fMarkUpDepth += 1
        if (fEntityScanner.skipChar('?')) {
          scanPI()
        } else if (fEntityScanner.skipChar('!')) {
          if (fEntityScanner.skipChar('-')) {
            if (!fEntityScanner.skipChar('-')) {
              reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
            } else {
              scanComment()
            }
          } else if (fEntityScanner.skipString("ELEMENT")) {
            scanElementDecl()
          } else if (fEntityScanner.skipString("ATTLIST")) {
            scanAttlistDecl()
          } else if (fEntityScanner.skipString("ENTITY")) {
            scanEntityDecl()
          } else if (fEntityScanner.skipString("NOTATION")) {
            scanNotationDecl()
          } else if (fEntityScanner.skipChar('[') && !scanningInternalSubset()) {
            scanConditionalSect(fPEDepth)
          } else {
            fMarkUpDepth -= 1
            reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
          }
        } else {
          fMarkUpDepth -= 1
          reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
        }
      } else if (fIncludeSectDepth > 0 && fEntityScanner.skipChar(']')) {
        if (!fEntityScanner.skipChar(']') || !fEntityScanner.skipChar('>')) {
          reportFatalError("IncludeSectUnterminated", null)
        }
        if (fDTDHandler ne null) {
          fDTDHandler.endConditional(null)
        }
        fIncludeSectDepth -= 1
        fMarkUpDepth -= 1
      } else if (scanningInternalSubset() && fEntityScanner.peekChar() == ']') {
        return false
      } else if (fEntityScanner.skipSpaces()) {
      } else {
        reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null)
        var ch: Int = 0
        do {
          fEntityScanner.scanChar()
          skipSeparator(spaceRequired = false, lookForPERefs = true)
          ch = fEntityScanner.peekChar()
        } while (ch != '<' && ch != ']' && !XMLChar.isSpace(ch))
      }
      skipSeparator(spaceRequired = false, lookForPERefs = true)
    }
    fScannerState != SCANNER_STATE_END_OF_INPUT
  }

  /**
   * Skip separator. This is typically just whitespace but it can also be one
   * or more parameter entity references.
   * 
   * If there are some it "expands them" by calling the corresponding entity
   * from the entity manager.
   * 
   * This is recursive and will process has many refs as possible.
   *
   * @param spaceRequired Specify whether some leading whitespace should be
   *                      found
   * @param lookForPERefs Specify whether parameter entity references should
   *                      be looked for
   * @return True if any leading whitespace was found or the end of a
   *         parameter entity was crossed.
   */
  private def skipSeparator(spaceRequired: Boolean, lookForPERefs: Boolean): Boolean = {
    val depth = fPEDepth
    val sawSpace = fEntityScanner.skipSpaces()
    if (!lookForPERefs || !fEntityScanner.skipChar('%')) {
      return !spaceRequired || sawSpace || (depth != fPEDepth)
    }
    while (true) {
      val name = fEntityScanner.scanName()
      if (name eq null) {
        reportFatalError("NameRequiredInPEReference", null)
      } else if (!fEntityScanner.skipChar(';')) {
        reportFatalError("SemicolonRequiredInPEReference", Array(name))
      }
      startPE(name, literal = false)
      fEntityScanner.skipSpaces()
      if (!fEntityScanner.skipChar('%'))
        return true
    }
    throw new IllegalStateException
  }

  private def pushContentStack(c: Int): Unit = {
    if (fContentStack.length == fContentDepth) {
      val newStack = new Array[Int](fContentDepth * 2)
      System.arraycopy(fContentStack, 0, newStack, 0, fContentDepth)
      fContentStack = newStack
    }
    fContentStack(fContentDepth) = c
    fContentDepth += 1
  }

  private def popContentStack(): Int = {
    fContentDepth -= 1
    fContentStack(fContentDepth)
  }

  private def pushPEStack(depth: Int, report: Boolean): Unit = {
    if (fPEStack.length == fPEDepth) {
      val newIntStack = new Array[Int](fPEDepth * 2)
      System.arraycopy(fPEStack, 0, newIntStack, 0, fPEDepth)
      fPEStack = newIntStack
      val newBooleanStack = new Array[Boolean](fPEDepth * 2)
      System.arraycopy(fPEReport, 0, newBooleanStack, 0, fPEDepth)
      fPEReport = newBooleanStack
    }
    fPEReport(fPEDepth) = report
    fPEStack(fPEDepth) = depth
    fPEDepth += 1
  }

  /**
   pop the stack
   */
  private def popPEStack(): Int = {
    fPEDepth -= 1
    fPEStack(fPEDepth)
  }

  /**
   look at the top of the stack
   */
  private def peekReportEntity(): Boolean = fPEReport(fPEDepth - 1)

  private def ensureEnumerationSize(size: Int): Unit = {
    if (fEnumeration.length == size) {
      val newEnum = new Array[String](size * 2)
      System.arraycopy(fEnumeration, 0, newEnum, 0, size)
      fEnumeration = newEnum
    }
  }

  private def init(): Unit = {
    fStartDTDCalled = false
    fExtEntityDepth = 0
    fIncludeSectDepth = 0
    fMarkUpDepth = 0
    fPEDepth = 0
    fStandalone = false
    fSeenExternalDTD = false
    fSeenPEReferences = false
    setScannerState(SCANNER_STATE_TEXT_DECL)
  }

  private def skipInvalidEnumerationValue(): Int = {
    var c: Int = 0
    do {
      c = fEntityScanner.scanChar()
    } while (c != '|' && c != ')')
    ensureEnumerationSize(fEnumerationCount + 1)
    fEnumeration(fEnumerationCount) = XMLSymbols.EMPTY_STRING
    fEnumerationCount += 1
    c
  }
}
