package org.apache.xerces.impl

import org.apache.xerces.impl.dtd.XMLDTDValidatorFilter
import org.apache.xerces.impl.msg.XMLMessageFormatter
import org.apache.xerces.util.XMLAttributesImpl
import org.apache.xerces.util.XMLSymbols
import org.apache.xerces.xni.NamespaceContext
import org.apache.xerces.xni.parser.XMLComponentManager

import scala.util.control.Breaks

/**
 * The scanner acts as the source for the document
 * information which is communicated to the document handler.
 *
 * This class scans an XML document, checks if document has a DTD, and if
 * DTD is not found the scanner will remove the DTD Validator from the pipeline and perform
 * namespace binding.
 *
 * Note: This scanner should only be used when the namespace processing is on!
 *
 * 
 * This component requires the following features and properties from the
 * component manager that uses it:
 * 
 *  - http://xml.org/sax/features/namespaces {true} -- if the value of this
 *      feature is set to false this scanner must not be used.
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
class XMLNSDocumentScannerImpl extends XMLDocumentScannerImpl {
  
  import XMLDocumentFragmentScannerImpl._
  import XMLDocumentScannerImpl._

  /**
   If is true, the dtd validator is no longer in the pipeline
   * and the scanner should bind namespaces
   */
  protected var fBindNamespaces: Boolean = _

  /**
   If validating parser, make sure we report an error in the
   *   scanner if DTD grammar is missing.
   */
  protected var fPerformValidation: Boolean = _

  /**
   DTD validator
   */
  private var fDTDValidator: XMLDTDValidatorFilter = _

  /**
   * Saw spaces after element name or between attributes.
   *
   * This is reserved for the case where scanning of a start element spans
   * several methods, as is the case when scanning the start of a root element
   * where a DTD external subset may be read after scanning the element name.
   */
  private var fSawSpace: Boolean = _

  /**
   * The scanner is responsible for removing DTD validator
   * from the pipeline if it is not needed.
   *
   * @param dtdValidator The DTDValidator
   */
  def setDTDValidator(dtdValidator: XMLDTDValidatorFilter): Unit = {
    fDTDValidator = dtdValidator
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
  override protected def scanStartElement(): Boolean = {
    if (DEBUG_CONTENT_SCANNING) println(">>> scanStartElementNS()")
    fEntityScanner.scanQName(fElementQName)
    val rawname = fElementQName.rawname
    if (fBindNamespaces) {
      fNamespaceContext.pushContext()
      if (fScannerState == SCANNER_STATE_ROOT_ELEMENT) {
        if (fPerformValidation) {
          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "MSG_GRAMMAR_NOT_FOUND", Array(rawname), 
            XMLErrorReporter.SEVERITY_ERROR)
          if ((fDoctypeName eq null) || fDoctypeName != rawname) {
            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "RootElementTypeMustMatchDoctypedecl", 
              Array(fDoctypeName, rawname), XMLErrorReporter.SEVERITY_ERROR)
          }
        }
      }
    }
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
          reportFatalError("ElementUnterminated", Array(rawname))
        }
        scanAttribute(fAttributes)
      } while (true)
    }
    if (fBindNamespaces) {
      if (fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", Array(fElementQName.rawname), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
      val prefix = if (fElementQName.prefix ne null) fElementQName.prefix else XMLSymbols.EMPTY_STRING
      fElementQName.uri = fNamespaceContext.getURI(prefix)
      fCurrentElement.uri = fElementQName.uri
      if ((fElementQName.prefix eq null) && (fElementQName.uri ne null)) {
        fElementQName.prefix = XMLSymbols.EMPTY_STRING
        fCurrentElement.prefix = XMLSymbols.EMPTY_STRING
      }
      if ((fElementQName.prefix ne null) && (fElementQName.uri eq null)) {
        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementPrefixUnbound", Array(fElementQName.prefix, fElementQName.rawname), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
      val length = fAttributes.getLength
      for (i ← 0 until length) {
        fAttributes.getName(i, fAttributeQName)
        val aprefix = if (fAttributeQName.prefix ne null) fAttributeQName.prefix else XMLSymbols.EMPTY_STRING
        val uri = fNamespaceContext.getURI(aprefix)
        if (! ((fAttributeQName.uri ne null) && fAttributeQName.uri == uri)) {
          if (aprefix != XMLSymbols.EMPTY_STRING) {
            fAttributeQName.uri = uri
            if (uri eq null) {
              fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributePrefixUnbound", Array(fElementQName.rawname, fAttributeQName.rawname, aprefix), 
                XMLErrorReporter.SEVERITY_FATAL_ERROR)
            }
            fAttributes.setURI(i, uri)
          }
        }
      }
      if (length > 1) {
        val name = fAttributes.checkDuplicatesNS()
        if (name ne null) {
          if (name.uri ne null) {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNSNotUnique", Array(fElementQName.rawname, name.localpart, name.uri), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          } else {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNotUnique", Array(fElementQName.rawname, name.rawname), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          }
        }
      }
    }
    if (fDocumentHandler ne null) {
      if (empty) {
        fMarkupDepth -= 1
        if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
          reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
        }
        fDocumentHandler.emptyElement(fElementQName, fAttributes, null)
        if (fBindNamespaces) {
          fNamespaceContext.popContext()
        }
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
  override protected def scanStartElementName(): Unit = {
    fEntityScanner.scanQName(fElementQName)
    fSawSpace = fEntityScanner.skipSpaces()
  }

  /**
   * Scans the remainder of a start or empty tag after the element name.
   *
   * @see #scanStartElement
   * @return True if element is empty.
   */
  override protected def scanStartElementAfterName(): Boolean = {
    val rawname = fElementQName.rawname
    if (fBindNamespaces) {
      fNamespaceContext.pushContext()
      if (fScannerState == SCANNER_STATE_ROOT_ELEMENT) {
        if (fPerformValidation) {
          fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "MSG_GRAMMAR_NOT_FOUND", Array(rawname), 
            XMLErrorReporter.SEVERITY_ERROR)
          if ((fDoctypeName eq null) || (fDoctypeName != rawname)) {
            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "RootElementTypeMustMatchDoctypedecl", 
              Array(fDoctypeName, rawname), XMLErrorReporter.SEVERITY_ERROR)
          }
        }
      }
    }
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
          reportFatalError("ElementUnterminated", Array(rawname))
        }
        scanAttribute(fAttributes)
        fSawSpace = fEntityScanner.skipSpaces()
      } while (true)
    }
    if (fBindNamespaces) {
      if (fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", Array(fElementQName.rawname), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
      val prefix = if (fElementQName.prefix ne null) fElementQName.prefix else XMLSymbols.EMPTY_STRING
      fElementQName.uri = fNamespaceContext.getURI(prefix)
      fCurrentElement.uri = fElementQName.uri
      if ((fElementQName.prefix eq null) && (fElementQName.uri ne null)) {
        fElementQName.prefix = XMLSymbols.EMPTY_STRING
        fCurrentElement.prefix = XMLSymbols.EMPTY_STRING
      }
      if ((fElementQName.prefix ne null) && (fElementQName.uri eq null)) {
        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementPrefixUnbound", Array(fElementQName.prefix, fElementQName.rawname), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
      val length = fAttributes.getLength
      for (i ← 0 until length) {
        fAttributes.getName(i, fAttributeQName)
        val aprefix = if (fAttributeQName.prefix ne null) fAttributeQName.prefix else XMLSymbols.EMPTY_STRING
        val uri = fNamespaceContext.getURI(aprefix)
        if (! ((fAttributeQName.uri ne null) && fAttributeQName.uri == uri)) {
          if (aprefix != XMLSymbols.EMPTY_STRING) {
            fAttributeQName.uri = uri
            if (uri eq null) {
              fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributePrefixUnbound", Array(fElementQName.rawname, fAttributeQName.rawname, aprefix), 
                XMLErrorReporter.SEVERITY_FATAL_ERROR)
            }
            fAttributes.setURI(i, uri)
          }
        }
      }
      if (length > 1) {
        val name = fAttributes.checkDuplicatesNS()
        if (name ne null) {
          if (name.uri ne null) {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNSNotUnique", Array(fElementQName.rawname, name.localpart, name.uri), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          } else {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNotUnique", Array(fElementQName.rawname, name.rawname), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          }
        }
      }
    }
    if (fDocumentHandler ne null) {
      if (empty) {
        fMarkupDepth -= 1
        if (fMarkupDepth < fEntityStack(fEntityDepth - 1)) {
          reportFatalError("ElementEntityMismatch", Array(fCurrentElement.rawname))
        }
        fDocumentHandler.emptyElement(fElementQName, fAttributes, null)
        if (fBindNamespaces) {
          fNamespaceContext.popContext()
        }
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
  protected def scanAttribute(attributes: XMLAttributesImpl): Unit = {
    if (DEBUG_CONTENT_SCANNING) println(">>> scanAttribute()")
    fEntityScanner.scanQName(fAttributeQName)
    fEntityScanner.skipSpaces()
    if (!fEntityScanner.skipChar('=')) {
      reportFatalError("EqRequiredInAttribute", Array(fCurrentElement.rawname, fAttributeQName.rawname))
    }
    fEntityScanner.skipSpaces()
    var attrIndex: Int = 0
    if (fBindNamespaces) {
      attrIndex = attributes.getLength
      attributes.addAttributeNS(fAttributeQName, XMLSymbols.fCDATASymbol, null)
    } else {
      val oldLen = attributes.getLength
      attrIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null)
      if (oldLen == attributes.getLength) {
        reportFatalError("AttributeNotUnique", Array(fCurrentElement.rawname, fAttributeQName.rawname))
      }
    }
    val isSameNormalizedAttr = scanAttributeValue(this.fTempString, fTempString2, fAttributeQName.rawname, 
      fIsEntityDeclaredVC, fCurrentElement.rawname)
    val value = fTempString.toString
    attributes.setValue(attrIndex, value)
    if (!isSameNormalizedAttr) {
      attributes.setNonNormalizedValue(attrIndex, fTempString2.toString)
    }
    attributes.setSpecified(attrIndex, specified = true)
    if (fBindNamespaces) {
      val localpart = fAttributeQName.localpart
      var prefix = if (fAttributeQName.prefix ne null) fAttributeQName.prefix else XMLSymbols.EMPTY_STRING
      if (prefix == XMLSymbols.PREFIX_XMLNS || 
        prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS) {
        val uri = fSymbolTable.addSymbol(value)
        if (prefix == XMLSymbols.PREFIX_XMLNS && localpart == XMLSymbols.PREFIX_XMLNS) {
          fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", Array(fAttributeQName), 
            XMLErrorReporter.SEVERITY_FATAL_ERROR)
        }
        if (uri == NamespaceContext.XMLNS_URI) {
          fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", Array(fAttributeQName), 
            XMLErrorReporter.SEVERITY_FATAL_ERROR)
        }
        if (localpart == XMLSymbols.PREFIX_XML) {
          if (uri != NamespaceContext.XML_URI) {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", Array(fAttributeQName), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          }
        } else {
          if (uri == NamespaceContext.XML_URI) {
            fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", Array(fAttributeQName), 
              XMLErrorReporter.SEVERITY_FATAL_ERROR)
          }
        }
        prefix = if (localpart != XMLSymbols.PREFIX_XMLNS) localpart else XMLSymbols.EMPTY_STRING
        if (uri == XMLSymbols.EMPTY_STRING && localpart != XMLSymbols.PREFIX_XMLNS) {
          fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "EmptyPrefixedAttName", Array(fAttributeQName), 
            XMLErrorReporter.SEVERITY_FATAL_ERROR)
        }
        fNamespaceContext.declarePrefix(prefix, if (uri.length != 0) uri else null)
        attributes.setURI(attrIndex, fNamespaceContext.getURI(XMLSymbols.PREFIX_XMLNS))
      } else {
        if (fAttributeQName.prefix ne null) {
          attributes.setURI(attrIndex, fNamespaceContext.getURI(fAttributeQName.prefix))
        }
      }
    }
    if (DEBUG_CONTENT_SCANNING) println("<<< scanAttribute()")
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
  override protected def scanEndElement(): Int = {
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
      if (fBindNamespaces) {
        fNamespaceContext.popContext()
      }
    }
    fMarkupDepth
  }

  override def reset(componentManager: XMLComponentManager): Unit = {
    super.reset(componentManager)
    fPerformValidation = false
    fBindNamespaces = false
  }

  /**
   Creates a content dispatcher.
   */
  override protected def createContentDispatcher(): Dispatcher = new NSContentDispatcher()

  /**
   * Dispatcher to handle content scanning.
   */
  protected class NSContentDispatcher extends ContentDispatcher {

    /**
     * Scan for root element hook. This method is a hook for
     * subclasses to add code that handles scanning for the root
     * element. This method will also attempt to remove DTD validator
     * from the pipeline, if there is no DTD grammar. If DTD validator
     * is no longer in the pipeline bind namespaces in the scanner.
     *
     *
     * @return True if the caller should stop and return true which
     *          allows the scanner to switch to a new scanning
     *          dispatcher. A return value of false indicates that
     *          the content dispatcher should continue as normal.
     */
    override protected def scanRootElementHook(): Boolean = {
      if ((fExternalSubsetResolver ne null) && !fSeenDoctypeDecl && 
        !fDisallowDoctype && (fValidation || fLoadExternalDTD)) {
        scanStartElementName()
        resolveExternalSubsetAndRead()
        reconfigurePipeline()
        if (scanStartElementAfterName()) {
          setScannerState(SCANNER_STATE_TRAILING_MISC)
          setDispatcher(fTrailingMiscDispatcher)
          return true
        }
      } else {
        reconfigurePipeline()
        if (scanStartElement()) {
          setScannerState(SCANNER_STATE_TRAILING_MISC)
          setDispatcher(fTrailingMiscDispatcher)
          return true
        }
      }
      false
    }

    /**
     * Re-configures pipeline by removing the DTD validator
     * if no DTD grammar exists. If no validator exists in the
     * pipeline or there is no DTD grammar, namespace binding
     * is performed by the scanner in the enclosing class.
     */
    private def reconfigurePipeline(): Unit = {
      if (fDTDValidator eq null) {
        fBindNamespaces = true
      } else if (!fDTDValidator.hasGrammar) {
        throw new UnsupportedOperationException
      }
    }
  }
}
