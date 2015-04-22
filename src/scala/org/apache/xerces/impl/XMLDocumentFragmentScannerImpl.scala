package org.apache.xerces.impl

import java.io.EOFException
import java.io.IOException

import org.apache.xerces.impl.XMLDocumentFragmentScannerImpl._
import org.apache.xerces.impl.io.MalformedByteSequenceException
import org.apache.xerces.impl.msg.XMLMessageFormatter
import org.apache.xerces.util.AugmentationsImpl
import org.apache.xerces.util.XMLAttributesImpl
import org.apache.xerces.util.XMLChar
import org.apache.xerces.util.XMLStringBuffer
import org.apache.xerces.util.XMLSymbols
import org.apache.xerces.xni.Augmentations
import org.apache.xerces.xni.QName
import org.apache.xerces.xni.XMLAttributes
import org.apache.xerces.xni.XMLDocumentHandler
import org.apache.xerces.xni.XMLResourceIdentifier
import org.apache.xerces.xni.XMLString
import org.apache.xerces.xni.XNIException
import org.apache.xerces.xni.parser.XMLComponent
import org.apache.xerces.xni.parser.XMLComponentManager
import org.apache.xerces.xni.parser.XMLConfigurationException
import org.apache.xerces.xni.parser.XMLDocumentScanner
import org.apache.xerces.xni.parser.XMLInputSource

import scala.util.control.Breaks

protected[impl] object XMLDocumentFragmentScannerImpl {

  /**
   Scanner state: start of markup.
   */
  val SCANNER_STATE_START_OF_MARKUP = 1

  /**
   Scanner state: comment.
   */
  val SCANNER_STATE_COMMENT = 2

  /**
   Scanner state: processing instruction.
   */
  val SCANNER_STATE_PI = 3

  /**
   Scanner state: DOCTYPE.
   */
  val SCANNER_STATE_DOCTYPE = 4

  /**
   Scanner state: root element.
   */
  val SCANNER_STATE_ROOT_ELEMENT = 6

  /**
   Scanner state: content.
   */
  val SCANNER_STATE_CONTENT = 7

  /**
   Scanner state: reference.
   */
  val SCANNER_STATE_REFERENCE = 8

  /**
   Scanner state: end of input.
   */
  val SCANNER_STATE_END_OF_INPUT = 13

  /**
   Scanner state: terminated.
   */
  val SCANNER_STATE_TERMINATED = 14

  /**
   Scanner state: CDATA section.
   */
  val SCANNER_STATE_CDATA = 15

  /**
   Scanner state: Text declaration.
   */
  val SCANNER_STATE_TEXT_DECL = 16

  /**
   Feature identifier: namespaces.
   */
  val NAMESPACES = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE

  /**
   Feature identifier: notify built-in refereces.
   */
  val NOTIFY_BUILTIN_REFS = Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_BUILTIN_REFS_FEATURE

  /**
   Property identifier: entity resolver.
   */
  val ENTITY_RESOLVER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY

  /**
   Recognized features.
   */
  private val RECOGNIZED_FEATURES = Array(NAMESPACES, XMLScanner.VALIDATION, NOTIFY_BUILTIN_REFS, XMLScanner.NOTIFY_CHAR_REFS)

  /**
   Feature defaults.
   */
  private val FEATURE_DEFAULTS = Array[java.lang.Boolean](null, null, false, false)

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(XMLScanner.SYMBOL_TABLE, XMLScanner.ERROR_REPORTER, XMLScanner.ENTITY_MANAGER, ENTITY_RESOLVER)

  /**
   Property defaults.
   */
  private val PROPERTY_DEFAULTS = Array(null, null, null, null)

  /**
   Debug scanner state.
   */
  private val DEBUG_SCANNER_STATE = false

  /**
   Debug dispatcher.
   */
  private val DEBUG_DISPATCHER = false

  /**
   Debug content dispatcher scanning.
   */
  val DEBUG_CONTENT_SCANNING = false

  /**
   * Element stack. This stack operates without synchronization, error
   * checking, and it re-uses objects instead of throwing popped items
   * away.
   */
  protected class ElementStack {

    /**
     The stack data.
     */
    protected var fElements: Array[QName] = new Array[QName](10)

    /**
     The size of the stack.
     */
    protected var fSize: Int = _

    for (i ← fElements.indices) {
      fElements(i) = new QName()
    }

    /**
     * Pushes an element on the stack.
     * 
     * *Note:* The QName values are copied into the
     * stack. In other words, the caller does *not* orphan
     * the element to the stack. Also, the QName object returned
     * is *not* orphaned to the caller. It should be
     * considered read-only.
     *
     * @param element The element to push onto the stack.
     *
     * @return Returns the actual QName object that stores the
     */
    def pushElement(element: QName): QName = {
      if (fSize == fElements.length) {
        val array = new Array[QName](fElements.length * 2)
        System.arraycopy(fElements, 0, array, 0, fSize)
        fElements = array
        for (i ← fSize until fElements.length) {
          fElements(i) = new QName()
        }
      }
      fElements(fSize).setValues(element)
      val result = fElements(fSize)
      fSize += 1
      result
    }

    /**
     * Pops an element off of the stack by setting the values of
     * the specified QName.
     * 
     * *Note:* The object returned is *not*
     * orphaned to the caller. Therefore, the caller should consider
     * the object to be read-only.
     */
    def popElement(element: QName): Unit = {
      fSize -= 1
      element.setValues(fElements(fSize))
    }

    /**
     Clears the stack without throwing away existing QName objects.
     */
    def clear(): Unit = {
      fSize = 0
    }
  }

  /**
   * This interface defines an XML "event" dispatching model. Classes
   * that implement this interface are responsible for scanning parts
   * of the XML document and dispatching callbacks.
   */
  protected[impl] trait Dispatcher {

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
    def dispatch(complete: Boolean): Boolean
  }
}

/**
 * This class is responsible for scanning the structure and content
 * of document fragments. The scanner acts as the source for the
 * document information which is communicated to the document handler.
 * 
 * This component requires the following features and properties from the
 * component manager that uses it:
 * 
 *  - http://xml.org/sax/features/validation
 *  - http://apache.org/xml/features/scanner/notify-char-refs
 *  - http://apache.org/xml/features/scanner/notify-builtin-refs
 *  - http://apache.org/xml/properties/internal/symbol-table
 *  - http://apache.org/xml/properties/internal/error-reporter
 *  - http://apache.org/xml/properties/internal/entity-manager
 * 
 */
class XMLDocumentFragmentScannerImpl extends XMLScanner with XMLDocumentScanner with XMLComponent with XMLEntityHandler {
  
  import XMLScanner._

  /**
   Document handler.
   */
  protected var fDocumentHandler: XMLDocumentHandler = _

  /**
   Entity stack.
   */
  protected var fEntityStack: Array[Int] = new Array[Int](4)

  /**
   Markup depth.
   */
  protected var fMarkupDepth: Int = _

  /**
   Scanner state.
   */
  protected var fScannerState: Int = _

  /**
   SubScanner state: inside scanContent method.
   */
  protected var fInScanContent: Boolean = false

  /**
   has external dtd
   */
  protected var fHasExternalDTD: Boolean = _

  /**
   Standalone.
   */
  protected var fStandalone: Boolean = _

  /**
   True if [Entity Declared] is a VC; false if it is a WFC.
   */
  protected var fIsEntityDeclaredVC: Boolean = _

  /**
   External subset resolver. *
   */
  protected var fExternalSubsetResolver: ExternalSubsetResolver = _

  /**
   Current element.
   */
  protected var fCurrentElement: QName = _

  /**
   Element stack.
   */
  protected val fElementStack = new ElementStack()

  /**
   Notify built-in references.
   */
  protected var fNotifyBuiltInRefs: Boolean = false

  /**
   Active dispatcher.
   */
  protected var fDispatcher: Dispatcher = _

  /**
   Content dispatcher.
   */
  protected val fContentDispatcher = createContentDispatcher()

  /**
   Element QName.
   */
  protected val fElementQName = new QName()

  /**
   Attribute QName.
   */
  protected val fAttributeQName = new QName()

  /**
   Element attributes.
   */
  protected val fAttributes = new XMLAttributesImpl()

  /**
   String.
   */
  protected val fTempString = new XMLString()

  /**
   String.
   */
  protected val fTempString2 = new XMLString()

  /**
   Array of 3 strings.
   */
  private val fStrings = new Array[String](3)

  /**
   String buffer.
   */
  private val fStringBuffer = new XMLStringBuffer()

  /**
   String buffer.
   */
  private val fStringBuffer2 = new XMLStringBuffer()

  /**
   Another QName.
   */
  private val fQName = new QName()

  /**
   Single character array.
   */
  private val fSingleChar = new Array[Char](1)

  /**
   * Saw spaces after element name or between attributes.
   *
   * This is reserved for the case where scanning of a start element spans
   * several methods, as is the case when scanning the start of a root element
   * where a DTD external subset may be read after scanning the element name.
   */
  private var fSawSpace: Boolean = _

  /**
   Reusable Augmentations.
   */
  private var fTempAugmentations: Augmentations = null

  /**
   * Sets the input source.
   *
   * @param inputSource The input source.
   *
   * @throws IOException Thrown on i/o error.
   */
  def setInputSource(inputSource: XMLInputSource): Unit = {
    fEntityManager.setEntityHandler(this)
    fEntityManager.startEntity("$fragment$", inputSource, literal = false, isExternal = true)
  }

  /**
   * Scans a document.
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
  def scanDocument(complete: Boolean): Boolean = {
    fEntityScanner = fEntityManager.getEntityScanner
    fEntityManager.setEntityHandler(this)
    do {
      if (!fDispatcher.dispatch(complete)) {
        return false
      }
    } while (complete)
    true
  }

  /**
   * Resets the component. The component can query the component manager
   * about any features and properties that affect the operation of the
   * component.
   *
   * @param componentManager The component manager.
   */
  override def reset(componentManager: XMLComponentManager): Unit = {
    super.reset(componentManager)
    fAttributes.setNamespaces(fNamespaces)
    fMarkupDepth = 0
    fCurrentElement = null
    fElementStack.clear()
    fHasExternalDTD = false
    fStandalone = false
    fIsEntityDeclaredVC = false
    fInScanContent = false
    setScannerState(SCANNER_STATE_CONTENT)
    setDispatcher(fContentDispatcher)
    if (fParserSettings) {
      fNotifyBuiltInRefs =
        try {
          componentManager.getFeature(NOTIFY_BUILTIN_REFS)
        } catch {
          case e: XMLConfigurationException ⇒ false
        }
      fExternalSubsetResolver =
        try {
          val resolver = componentManager.getProperty(ENTITY_RESOLVER)
          resolver match {
            case externalResolver: ExternalSubsetResolver ⇒ externalResolver
            case _ ⇒ null
          }
        } catch {
          case e: XMLConfigurationException ⇒ null
        }
    }
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
   * Sets the state of a feature. This method is called by the component
   * manager any time after reset when a feature changes state.
   * 
   * *Note:* Components should silently ignore features
   * that do not affect the operation of the component.
   *
   * @param featureId The feature identifier.
   * @param state     The state of the feature.
   */
  override def setFeature(featureId: String, state: Boolean): Unit = {
    super.setFeature(featureId, state)
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == Constants.NOTIFY_BUILTIN_REFS_FEATURE.length && 
        featureId.endsWith(Constants.NOTIFY_BUILTIN_REFS_FEATURE)) {
        fNotifyBuiltInRefs = state
      }
    }
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
   * Sets the value of a property. This method is called by the component
   * manager any time after reset when a property changes value.
   * 
   * *Note:* Components should silently ignore properties
   * that do not affect the operation of the component.
   *
   * @param propertyId The property identifier.
   * @param value      The value of the property.
   */
  override def setProperty(propertyId: String, value: AnyRef): Unit = {
    super.setProperty(propertyId, value)
    if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.XERCES_PROPERTY_PREFIX.length
      if (suffixLength == Constants.ENTITY_MANAGER_PROPERTY.length && 
        propertyId.endsWith(Constants.ENTITY_MANAGER_PROPERTY)) {
        fEntityManager = value.asInstanceOf[XMLEntityManager]
        return
      }
      if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length && 
        propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
        fExternalSubsetResolver = value match {
          case resolver: ExternalSubsetResolver ⇒ resolver
          case _ ⇒ null
        }
        return
      }
    }
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

  /**
   * setDocumentHandler
   */
  def setDocumentHandler(documentHandler: XMLDocumentHandler): Unit = {
    fDocumentHandler = documentHandler
  }

  /**
   Returns the document handler
   */
  def getDocumentHandler: XMLDocumentHandler = fDocumentHandler

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
    if (fEntityDepth == fEntityStack.length) {
      val entityarray = new Array[Int](fEntityStack.length * 2)
      System.arraycopy(fEntityStack, 0, entityarray, 0, fEntityStack.length)
      fEntityStack = entityarray
    }
    fEntityStack(fEntityDepth) = fMarkupDepth
    super.startEntity(name, identifier, encoding, augs)
    if (fStandalone && fEntityManager.isEntityDeclInExternalSubset(name)) {
      reportFatalError("MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE", Array(name))
    }
    if ((fDocumentHandler ne null) && !fScanningAttribute) {
      if (name != "[xml]") {
        fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs)
      }
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
    if (fInScanContent && fStringBuffer.length != 0 && (fDocumentHandler ne null)) {
      fDocumentHandler.characters(fStringBuffer, null)
      fStringBuffer.length = 0
    }
    super.endEntity(name, augs)
    if (fMarkupDepth != fEntityStack(fEntityDepth)) {
      reportFatalError("MarkupEntityMismatch", null)
    }
    if ((fDocumentHandler ne null) && ! fScanningAttribute) {
      if (name != "[xml]") {
        fDocumentHandler.endGeneralEntity(name, augs)
      }
    }
  }

  /**
   Creates a content dispatcher.
   */
  protected def createContentDispatcher(): Dispatcher = new FragmentContentDispatcher()

  /**
   * Scans an XML or text declaration.
   * 
   *    [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
   *    [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
   *    [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
   *    [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
   *    [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
   *                    | ('"' ('yes' | 'no') '"'))
   *    
   *    [77] TextDecl ::= '<?xml' VersionInfo? EncodingDecl S? '?>'
   *
   * @param scanningTextDecl True if a text declaration is to
   *                         be scanned instead of an XML
   *                         declaration.
   */
  protected def scanXMLDeclOrTextDecl(scanningTextDecl: Boolean): Unit = {
    super.scanXMLDeclOrTextDecl(scanningTextDecl, fStrings)
    fMarkupDepth -= 1
    val version = fStrings(0)
    val encoding = fStrings(1)
    val standalone = fStrings(2)
    fStandalone = standalone == "yes"
    fEntityManager.setStandalone(fStandalone)
    fEntityScanner.setXMLVersion(version)
    if (fDocumentHandler ne null) {
      if (scanningTextDecl) {
        fDocumentHandler.textDecl(version, encoding, null)
      } else {
        fDocumentHandler.xmlDecl(version, encoding, standalone, null)
      }
    }
    if ((encoding ne null) && ! fEntityScanner.fCurrentEntity.isEncodingExternallySpecified) {
      fEntityScanner.setEncoding(encoding)
    }
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
    fMarkupDepth -= 1
    if (fDocumentHandler ne null) {
      fDocumentHandler.processingInstruction(target, data, null)
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
    scanComment(fStringBuffer)
    fMarkupDepth -= 1
    if (fDocumentHandler ne null) {
      fDocumentHandler.comment(fStringBuffer, null)
    }
  }

  /**
   * Scans a start element. This method will handle the binding of
   * namespace information and notifying the handler of the start
   * of the element.
   * 
   *  [44] EmptyElemTag ::= '<' Name (S Attribute)* S? '/>'
   *  [40] STag ::= '<' Name (S Attribute)* S? '>'
   * 
   * *Note:* This method assumes that the leading
   * '<' character has been consumed.
   * 
   * *Note:* This method uses the fElementQName and
   * fAttributes variables. The contents of these variables will be
   * destroyed. The caller should copy important information out of
   * these variables before calling this method.
   *
   * @return True if element is empty. (i.e. It matches
   *          production [44].
   */
  protected def scanStartElement(): Boolean = {
    if (DEBUG_CONTENT_SCANNING) println(">>> scanStartElement()")
    if (fNamespaces) {
      fEntityScanner.scanQName(fElementQName)
    } else {
      val name = fEntityScanner.scanName()
      fElementQName.setValues(null, name, name, null)
    }
    val rawname = fElementQName.rawname
    fCurrentElement = fElementStack.pushElement(fElementQName)
    var empty = false
    fAttributes.removeAllAttributes()
    val doBreaks = new Breaks
    doBreaks.breakable {
      do {
        val sawSpace = fEntityScanner.skipSpaces()
        val c = fEntityScanner.peekChar()
        if (c == '>') {
          fEntityScanner.scanChar()
          doBreaks.break()
        } else if (c == '/') {
          fEntityScanner.scanChar()
          if (!fEntityScanner.skipChar('>')) {
            reportFatalError("ElementUnterminated", Array(rawname))
          }
          empty = true
          doBreaks.break()
        } else if (!isValidNameStartChar(c) || !sawSpace) {
          if (!isValidNameStartHighSurrogate(c) || !sawSpace) {
            reportFatalError("ElementUnterminated", Array(rawname))
          }
        }
        scanAttribute(fAttributes)
      } while (true)
    }
    if (fDocumentHandler ne null) {
      if (empty) {
        fMarkupDepth -= 1
        if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
          reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
        }
        fDocumentHandler.emptyElement(fElementQName, fAttributes, null)
        fElementStack.popElement(fElementQName)
      } else {
        fDocumentHandler.startElement(fElementQName, fAttributes, null)
      }
    }
    if (DEBUG_CONTENT_SCANNING) println("<<< scanStartElement(): " + empty)
    empty
  }

  /**
   * Scans the name of an element in a start or empty tag.
   *
   * @see #scanStartElement()
   */
  protected def scanStartElementName(): Unit = {
    if (fNamespaces) {
      fEntityScanner.scanQName(fElementQName)
    } else {
      val name = fEntityScanner.scanName()
      fElementQName.setValues(null, name, name, null)
    }
    fSawSpace = fEntityScanner.skipSpaces()
  }

  /**
   * Scans the remainder of a start or empty tag after the element name.
   *
   * @see #scanStartElement
   * @return True if element is empty.
   */
  protected def scanStartElementAfterName(): Boolean = {
    val rawname = fElementQName.rawname
    fCurrentElement = fElementStack.pushElement(fElementQName)
    var empty = false
    fAttributes.removeAllAttributes()
    val doBreaks = new Breaks
    doBreaks.breakable {
      do {
        val c = fEntityScanner.peekChar()
        if (c == '>') {
          fEntityScanner.scanChar()
          doBreaks.break()
        } else if (c == '/') {
          fEntityScanner.scanChar()
          if (!fEntityScanner.skipChar('>')) {
            reportFatalError("ElementUnterminated", Array(rawname))
          }
          empty = true
          doBreaks.break()
        } else if (!isValidNameStartChar(c) || !fSawSpace) {
          if (!isValidNameStartHighSurrogate(c) || !fSawSpace) {
            reportFatalError("ElementUnterminated", Array(rawname))
          }
        }
        scanAttribute(fAttributes)
        fSawSpace = fEntityScanner.skipSpaces()
      } while (true)
    }
    if (fDocumentHandler ne null) {
      if (empty) {
        fMarkupDepth -= 1
        if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
          reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
        }
        fDocumentHandler.emptyElement(fElementQName, fAttributes, null)
        fElementStack.popElement(fElementQName)
      } else {
        fDocumentHandler.startElement(fElementQName, fAttributes, null)
      }
    }
    if (DEBUG_CONTENT_SCANNING) println("<<< scanStartElementAfterName(): " + empty)
    empty
  }

  /**
   * Scans an attribute.
   * 
   *  [41] Attribute ::= Name Eq AttValue
   * 
   * *Note:* This method assumes that the next
   * character on the stream is the first character of the attribute
   * name.
   * 
   * *Note:* This method uses the fAttributeQName and
   * fQName variables. The contents of these variables will be
   * destroyed.
   *
   * @param attributes The attributes list for the scanned attribute.
   */
  protected def scanAttribute(attributes: XMLAttributes): Unit = {
    if (DEBUG_CONTENT_SCANNING) println(">>> scanAttribute()")
    if (fNamespaces) {
      fEntityScanner.scanQName(fAttributeQName)
    } else {
      val name = fEntityScanner.scanName()
      fAttributeQName.setValues(null, name, name, null)
    }
    fEntityScanner.skipSpaces()
    if (!fEntityScanner.skipChar('=')) {
      reportFatalError("EqRequiredInAttribute", Array(fCurrentElement.rawname, fAttributeQName.rawname))
    }
    fEntityScanner.skipSpaces()
    val oldLen = attributes.getLength
    val attrIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null)
    if (oldLen == attributes.getLength) {
      reportFatalError("AttributeNotUnique", Array(fCurrentElement.rawname, fAttributeQName.rawname))
    }
    val isSameNormalizedAttr = scanAttributeValue(fTempString, fTempString2, fAttributeQName.rawname, 
      fIsEntityDeclaredVC, fCurrentElement.rawname)
    attributes.setValue(attrIndex, fTempString.toString)
    if (!isSameNormalizedAttr) {
      attributes.setNonNormalizedValue(attrIndex, fTempString2.toString)
    }
    attributes.setSpecified(attrIndex, specified = true)
    if (DEBUG_CONTENT_SCANNING) println("<<< scanAttribute()")
  }

  /**
   * Scans element content.
   *
   * @return Returns the next character on the stream.
   */
  protected def scanContent(): Int = {
    var content = fTempString
    var c = fEntityScanner.scanContent(content)
    if (c == '\r') {
      fEntityScanner.scanChar()
      fStringBuffer.clear()
      fStringBuffer.append(fTempString)
      fStringBuffer.append(c.toChar)
      content = fStringBuffer
      c = -1
    }
    if ((fDocumentHandler ne null) && content.length > 0) {
      fDocumentHandler.characters(content, null)
    }
    if (c == ']' && fTempString.length == 0) {
      fStringBuffer.clear()
      fStringBuffer.append(fEntityScanner.scanChar().toChar)
      fInScanContent = true
      if (fEntityScanner.skipChar(']')) {
        fStringBuffer.append(']')
        while (fEntityScanner.skipChar(']')) {
          fStringBuffer.append(']')
        }
        if (fEntityScanner.skipChar('>')) {
          reportFatalError("CDEndInContent", null)
        }
      }
      if ((fDocumentHandler ne null) && fStringBuffer.length != 0) {
        fDocumentHandler.characters(fStringBuffer, null)
      }
      fInScanContent = false
      c = -1
    }
    c
  }

  /**
   * Scans a CDATA section.
   * 
   * *Note:* This method uses the fTempString and
   * fStringBuffer variables.
   *
   * @param complete True if the CDATA section is to be scanned
   *                 completely.
   *
   * @return True if CDATA is completely scanned.
   */
  protected def scanCDATASection(complete: Boolean): Boolean = {
    if (fDocumentHandler ne null) {
      fDocumentHandler.startCDATA(null)
    }
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      while (true) {
        fStringBuffer.clear()
        if (!fEntityScanner.scanData("]]", fStringBuffer)) {
          if ((fDocumentHandler ne null) && fStringBuffer.length > 0) {
            fDocumentHandler.characters(fStringBuffer, null)
          }
          var brackets = 0
          while (fEntityScanner.skipChar(']')) {
            brackets += 1
          }
          if ((fDocumentHandler ne null) && brackets > 0) {
            fStringBuffer.clear()
            if (brackets > XMLEntityManager.DEFAULT_BUFFER_SIZE) {
              val chunks = brackets / XMLEntityManager.DEFAULT_BUFFER_SIZE
              val remainder = brackets % XMLEntityManager.DEFAULT_BUFFER_SIZE
              for (i ← 0 until XMLEntityManager.DEFAULT_BUFFER_SIZE) {
                fStringBuffer.append(']')
              }
              for (i ← 0 until chunks) {
                fDocumentHandler.characters(fStringBuffer, null)
              }
              if (remainder != 0) {
                fStringBuffer.length = remainder
                fDocumentHandler.characters(fStringBuffer, null)
              }
            } else {
              for (i ← 0 until brackets) {
                fStringBuffer.append(']')
              }
              fDocumentHandler.characters(fStringBuffer, null)
            }
          }
          if (fEntityScanner.skipChar('>')) {
            whileBreaks.break()
          }
          if (fDocumentHandler ne null) {
            fStringBuffer.clear()
            fStringBuffer.append("]]")
            fDocumentHandler.characters(fStringBuffer, null)
          }
        } else {
          if (fDocumentHandler ne null) {
            fDocumentHandler.characters(fStringBuffer, null)
          }
          val c = fEntityScanner.peekChar()
          if (c != -1 && isInvalidLiteral(c)) {
            if (XMLChar.isHighSurrogate(c)) {
              fStringBuffer.clear()
              scanSurrogates(fStringBuffer)
              if (fDocumentHandler ne null) {
                fDocumentHandler.characters(fStringBuffer, null)
              }
            } else {
              reportFatalError("InvalidCharInCDSect", Array(Integer.toString(c, 16)))
              fEntityScanner.scanChar()
            }
          }
        }
      }
    }
    fMarkupDepth -= 1
    if (fDocumentHandler ne null) {
      fDocumentHandler.endCDATA(null)
    }
    true
  }

  /**
   * Scans an end element.
   * 
   *  [42] ETag ::= '</' Name S? '>'
   * 
   * *Note:* This method uses the fElementQName variable.
   * The contents of this variable will be destroyed. The caller should
   * copy the needed information out of this variable before calling
   * this method.
   *
   * @return The element depth.
   */
  protected def scanEndElement(): Int = {
    if (DEBUG_CONTENT_SCANNING) println(">>> scanEndElement()")
    fElementStack.popElement(fElementQName)
    if (!fEntityScanner.skipString(fElementQName.rawname)) {
      reportFatalError("ETagRequired", Array(fElementQName.rawname))
    }
    fEntityScanner.skipSpaces()
    if (!fEntityScanner.skipChar('>')) {
      reportFatalError("ETagUnterminated", Array(fElementQName.rawname))
    }
    fMarkupDepth -= 1
    fMarkupDepth -= 1
    if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
      reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
    }
    if (fDocumentHandler ne null) {
      fDocumentHandler.endElement(fElementQName, null)
    }
    fMarkupDepth
  }

  /**
   * Scans a character reference.
   * 
   *  [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
   */
  protected def scanCharReference(): Unit = {
    fStringBuffer2.clear()
    val ch = scanCharReferenceValue(fStringBuffer2, null)
    fMarkupDepth -= 1
    if (ch != -1) {
      if (fDocumentHandler ne null) {
        if (fNotifyCharRefs) {
          fDocumentHandler.startGeneralEntity(fCharRefLiteral, null, null, null)
        }
        var augs: Augmentations = null
        if (fValidation && ch <= 0x20) {
          if (fTempAugmentations ne null) {
            fTempAugmentations.removeAllItems()
          } else {
            fTempAugmentations = new AugmentationsImpl
          }
          augs = fTempAugmentations
          augs.putItem(Constants.CHAR_REF_PROBABLE_WS, true)
        }
        fDocumentHandler.characters(fStringBuffer2, augs)
        if (fNotifyCharRefs) {
          fDocumentHandler.endGeneralEntity(fCharRefLiteral, null)
        }
      }
    }
  }

  /**
   * Scans an entity reference.
   *
   * @throws IOException  Thrown if i/o error occurs.
   * @throws XNIException Thrown if handler throws exception upon
   *                      notification.
   */
  protected def scanEntityReference(): Unit = {
    val name = fEntityScanner.scanName()
    if (name eq null) {
      reportFatalError("NameRequiredInReference", null)
      return
    }
    if (!fEntityScanner.skipChar(';')) {
      reportFatalError("SemicolonRequiredInReference", Array(name))
    }
    fMarkupDepth -= 1
    if (name == fAmpSymbol) {
      handleCharacter('&', fAmpSymbol)
    } else if (name == fLtSymbol) {
      handleCharacter('<', fLtSymbol)
    } else if (name == fGtSymbol) {
      handleCharacter('>', fGtSymbol)
    } else if (name == fQuotSymbol) {
      handleCharacter('"', fQuotSymbol)
    } else if (name == fAposSymbol) {
      handleCharacter('\'', fAposSymbol)
    } else if (fEntityManager.isUnparsedEntity(name)) {
      reportFatalError("ReferenceToUnparsedEntity", Array(name))
    } else {
      if (!fEntityManager.isDeclaredEntity(name)) {
        if (fIsEntityDeclaredVC) {
          if (fValidation) fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EntityNotDeclared", 
            Array(name), XMLErrorReporter.SEVERITY_ERROR)
        } else {
          reportFatalError("EntityNotDeclared", Array(name))
        }
      }
      fEntityManager.startEntity(name, literal = false)
    }
  }

  /**
   * Calls document handler with a single character resulting from
   * built-in entity resolution.
   *
   * @param entity built-in name
   */
  private def handleCharacter(c: Char, entity: String): Unit = {
    if (fDocumentHandler ne null) {
      if (fNotifyBuiltInRefs) {
        fDocumentHandler.startGeneralEntity(entity, null, null, null)
      }
      fSingleChar(0) = c
      fTempString.setValues(fSingleChar, 0, 1)
      fDocumentHandler.characters(fTempString, null)
      if (fNotifyBuiltInRefs) {
        fDocumentHandler.endGeneralEntity(entity, null)
      }
    }
  }

  protected def handleEndElement(element: QName, isEmpty: Boolean): Int = {
    fMarkupDepth -= 1
    if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
      reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
    }
    val startElement = fQName
    fElementStack.popElement(startElement)
    if (element.rawname != startElement.rawname) {
      reportFatalError("ETagRequired", Array(startElement.rawname))
    }
    if (fNamespaces) {
      element.uri = startElement.uri
    }
    if ((fDocumentHandler ne null) && ! isEmpty) {
      fDocumentHandler.endElement(element, null)
    }
    fMarkupDepth
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

  /**
   * Sets the dispatcher.
   *
   * @param dispatcher The new dispatcher.
   */
  protected def setDispatcher(dispatcher: Dispatcher): Unit = {
    fDispatcher = dispatcher
    if (DEBUG_DISPATCHER) {
      System.out.print("%%% setDispatcher: ")
      System.out.print(getDispatcherName(dispatcher))
      println()
    }
  }

  /**
   Returns the scanner state name.
   */
  protected def getScannerStateName(state: Int): String = state match {
    case SCANNER_STATE_DOCTYPE ⇒ "SCANNER_STATE_DOCTYPE"
    case SCANNER_STATE_ROOT_ELEMENT ⇒ "SCANNER_STATE_ROOT_ELEMENT"
    case SCANNER_STATE_START_OF_MARKUP ⇒ "SCANNER_STATE_START_OF_MARKUP"
    case SCANNER_STATE_COMMENT ⇒ "SCANNER_STATE_COMMENT"
    case SCANNER_STATE_PI ⇒ "SCANNER_STATE_PI"
    case SCANNER_STATE_CONTENT ⇒ "SCANNER_STATE_CONTENT"
    case SCANNER_STATE_REFERENCE ⇒ "SCANNER_STATE_REFERENCE"
    case SCANNER_STATE_END_OF_INPUT ⇒ "SCANNER_STATE_END_OF_INPUT"
    case SCANNER_STATE_TERMINATED ⇒ "SCANNER_STATE_TERMINATED"
    case SCANNER_STATE_CDATA ⇒ "SCANNER_STATE_CDATA"
    case SCANNER_STATE_TEXT_DECL ⇒ "SCANNER_STATE_TEXT_DECL"
  }

  /**
   Returns the dispatcher name.
   */
  def getDispatcherName(dispatcher: Dispatcher): String = {
    if (DEBUG_DISPATCHER) {
      if (dispatcher ne null) {
        var name = dispatcher.getClass.getName
        var index = name.lastIndexOf('.')
        if (index != -1) {
          name = name.substring(index + 1)
          index = name.lastIndexOf('$')
          if (index != -1) {
            name = name.substring(index + 1)
          }
        }
        return name
      }
    }
    "null"
  }

  /**
   * Dispatcher to handle content scanning.
   */
  protected class FragmentContentDispatcher extends Dispatcher {

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
            case SCANNER_STATE_CONTENT ⇒
              if (fEntityScanner.skipChar('<')) {
                setScannerState(SCANNER_STATE_START_OF_MARKUP)
                again = true
              } else if (fEntityScanner.skipChar('&')) {
                setScannerState(SCANNER_STATE_REFERENCE)
                again = true
              } else {
                val doBreaks = new Breaks
                doBreaks.breakable {
                  do {
                    val c = scanContent()
                    if (c == '<') {
                      fEntityScanner.scanChar()
                      setScannerState(SCANNER_STATE_START_OF_MARKUP)
                      doBreaks.break()
                    } else if (c == '&') {
                      fEntityScanner.scanChar()
                      setScannerState(SCANNER_STATE_REFERENCE)
                      doBreaks.break()
                    } else if (c != -1 && isInvalidLiteral(c)) {
                      if (XMLChar.isHighSurrogate(c)) {
                        fStringBuffer.clear()
                        if (scanSurrogates(fStringBuffer)) {
                          if (fDocumentHandler ne null) {
                            fDocumentHandler.characters(fStringBuffer, null)
                          }
                        }
                      } else {
                        reportFatalError("InvalidCharInContent", Array(Integer.toString(c, 16)))
                        fEntityScanner.scanChar()
                      }
                    }
                  } while (complete)
                }
              }
            case SCANNER_STATE_START_OF_MARKUP ⇒
              fMarkupDepth += 1
              if (fEntityScanner.skipChar('/')) {
                if (scanEndElement() == 0) {
                  if (elementDepthIsZeroHook()) {
                    return true
                  }
                }
                setScannerState(SCANNER_STATE_CONTENT)
              } else if (isValidNameStartChar(fEntityScanner.peekChar())) {
                scanStartElement()
                setScannerState(SCANNER_STATE_CONTENT)
              } else if (fEntityScanner.skipChar('!')) {
                if (fEntityScanner.skipChar('-')) {
                  if (!fEntityScanner.skipChar('-')) {
                    reportFatalError("InvalidCommentStart", null)
                  }
                  setScannerState(SCANNER_STATE_COMMENT)
                  again = true
                } else if (fEntityScanner.skipString("[CDATA[")) {
                  setScannerState(SCANNER_STATE_CDATA)
                  again = true
                } else if (!scanForDoctypeHook()) {
                  reportFatalError("MarkupNotRecognizedInContent", null)
                }
              } else if (fEntityScanner.skipChar('?')) {
                setScannerState(SCANNER_STATE_PI)
                again = true
              } else if (isValidNameStartHighSurrogate(fEntityScanner.peekChar())) {
                scanStartElement()
                setScannerState(SCANNER_STATE_CONTENT)
              } else {
                reportFatalError("MarkupNotRecognizedInContent", null)
                setScannerState(SCANNER_STATE_CONTENT)
              }
            case SCANNER_STATE_COMMENT ⇒
              scanComment()
              setScannerState(SCANNER_STATE_CONTENT)
            case SCANNER_STATE_PI ⇒
              scanPI()
              setScannerState(SCANNER_STATE_CONTENT)
            case SCANNER_STATE_CDATA ⇒
              scanCDATASection(complete)
              setScannerState(SCANNER_STATE_CONTENT)
            case SCANNER_STATE_REFERENCE ⇒
              fMarkupDepth += 1
              setScannerState(SCANNER_STATE_CONTENT)
              if (fEntityScanner.skipChar('#')) {
                scanCharReference()
              } else {
                scanEntityReference()
              }
            case SCANNER_STATE_TEXT_DECL ⇒
              if (fEntityScanner.skipString("<?xml")) {
                fMarkupDepth += 1
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
                  scanPIData(target, fTempString)
                } else {
                  scanXMLDeclOrTextDecl(scanningTextDecl = true)
                }
              }
              fEntityManager.fCurrentEntity.mayReadChunks = true
              setScannerState(SCANNER_STATE_CONTENT)
            case SCANNER_STATE_ROOT_ELEMENT ⇒
              if (scanRootElementHook()) {
                return true
              }
              setScannerState(SCANNER_STATE_CONTENT)
            case SCANNER_STATE_DOCTYPE ⇒
              reportFatalError("DoctypeIllegalInContent", null)
              setScannerState(SCANNER_STATE_CONTENT)
          }
        } while (complete || again)
      } catch {
        case e: MalformedByteSequenceException ⇒
          fErrorReporter.reportError(e.getDomain, e.getKey, e.getArguments, XMLErrorReporter.SEVERITY_FATAL_ERROR, 
            e)
          return false
// @ebruchez: not supported in Scala.js
//        case e: CharConversionException ⇒
//          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR, 
//            e)
//          return false
        case e: EOFException ⇒
          endOfFileHook(e)
          return false
      }
      true
    }

    /**
     * Scan for DOCTYPE hook. This method is a hook for subclasses
     * to add code to handle scanning for a the "DOCTYPE" string
     * after the string "<!" has been scanned.
     *
     * @return True if the "DOCTYPE" was scanned; false if "DOCTYPE"
     *          was not scanned.
     */
    protected def scanForDoctypeHook(): Boolean = false

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
    protected def elementDepthIsZeroHook(): Boolean = false

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
    protected def scanRootElementHook(): Boolean = false

    /**
     * End of file hook. This method is a hook for subclasses to
     * add code that handles the end of file. The end of file in
     * a document fragment is OK if the markup depth is zero.
     * However, when scanning a full XML document, an end of file
     * is always premature.
     */
    protected def endOfFileHook(e: EOFException): Unit = {
      if (fMarkupDepth != 0) {
        reportFatalError("PrematureEOF", null)
      }
    }
  }
}
