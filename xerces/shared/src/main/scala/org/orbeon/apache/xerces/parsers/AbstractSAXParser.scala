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
package org.orbeon.apache.xerces.parsers

import com.sun.org.apache.xerces.internal.util.{EntityResolver2Wrapper, EntityResolverWrapper, ErrorHandlerWrapper}
import org.orbeon.apache.xerces.impl.Constants
import org.orbeon.apache.xerces.util.{SymbolHash, XMLSymbols}
import org.orbeon.apache.xerces.xni._
import org.orbeon.apache.xerces.xni.parser._
import org.xml.sax._
import org.xml.sax.ext._

import java.io.{CharConversionException, IOException}


abstract class AbstractSAXParser(config: XMLParserConfiguration)
  extends AbstractXMLDocumentParser(config)
     with XMLReader {

  import AbstractSAXParser._

  //
  // Data
  //
  // features
  /** Namespaces. */
  protected var fNamespaces = false

  /** Namespace prefixes. */
  protected var fNamespacePrefixes = false

  /** Lexical handler parameter entities. */
  protected var fLexicalHandlerParameterEntities = true

  /** Standalone document declaration. */
  protected var fStandalone = false

  /** Resolve DTD URIs. */
  protected var fResolveDTDURIs = true

  /** Use EntityResolver2. */
  protected var fUseEntityResolver2 = true

  /**
   * XMLNS URIs: Namespace declarations in the
   * http://www.w3.org/2000/xmlns/ namespace.
   */
  protected var fXMLNSURIs = false

  // parser handlers

  /** Content handler. */
  protected var fContentHandler: ContentHandler = null

  /** Namespace context */
  protected var fNamespaceContext: NamespaceContext = null

  /** DTD handler. */
  protected var fDTDHandler: DTDHandler = null

  /** Decl handler. */
  protected var fDeclHandler: DeclHandler = null

  /** Lexical handler. */
  protected var fLexicalHandler: LexicalHandler = null

  protected val fQName = new QName

  // state

  /**
   * True if a parse is in progress. This state is needed because
   * some features/properties cannot be set while parsing (e.g.
   * validation and namespaces).
   */
  protected var fParseInProgress = false

  // track the version of the document being parsed
  protected var fVersion: String = null

  // temp vars
  private val fAttributesProxy = new AttributesProxy
  private var fAugmentations: Augmentations = null

  // allows us to keep track of whether an attribute has
  // been declared twice, so that we can avoid exposing the
  // second declaration to any registered DeclHandler
  protected var fDeclaredAttrs: SymbolHash = null


  locally {
    config.addRecognizedFeatures(RECOGNIZED_FEATURES)
    config.addRecognizedProperties(RECOGNIZED_PROPERTIES)
    try
      config.setFeature(ALLOW_UE_AND_NOTATION_EVENTS, false)
    catch {
      case _: XMLConfigurationException ⇒
      // it wasn't a recognized feature, so we don't worry about it
    }
  }

  override def startDocument(
    locator         : XMLLocator,
    encoding        : String,
    namespaceContext: NamespaceContext,
    augs            : Augmentations
  ): Unit = {
    fNamespaceContext = namespaceContext
    try {
      if (fContentHandler != null) {
        if (locator != null)
          fContentHandler.setDocumentLocator(new LocatorProxy(locator))
        // The application may have set the ContentHandler to null
        if (fContentHandler != null) fContentHandler.startDocument()
      }
    } catch {
      case e: SAXException ⇒
        throw new XNIException(e)
    }
  }

  override def xmlDecl(
    version    : String,
    encoding   : String,
    standalone : String,
    augs       : Augmentations
  ): Unit = {
    // the version need only be set once; if document's XML 1.0|1.1, that's how it'll stay
    fVersion = version
    fStandalone = "yes" == standalone
  }

  override def doctypeDecl(
    rootElement: String,
    publicId   : String,
    systemId   : String,
    augs       : Augmentations
  ): Unit = {
    fInDTD = true
    try
      if (fLexicalHandler != null)
        fLexicalHandler.startDTD(rootElement, publicId, systemId)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }
    if (fDeclHandler != null)
      fDeclaredAttrs = new SymbolHash
  }

  override def startGeneralEntity(
    name      : String,
    identifier: XMLResourceIdentifier,
    encoding  : String,
    augs      : Augmentations
  ): Unit =
    try {
    // Only report startEntity if this entity was actually read.
    if (augs != null && true == augs.getItem(Constants.ENTITY_SKIPPED)) {
      // report skipped entity to content handler
      if (fContentHandler != null)
        fContentHandler.skippedEntity(name)
    } else if (fLexicalHandler != null)
      fLexicalHandler.startEntity(name)
    } catch {
    case e: SAXException =>
      throw new XNIException(e)
  }

  override def endGeneralEntity(
    name: String,
    augs: Augmentations
  ): Unit =
    try  {
      // Only report endEntity if this entity was actually read.
      if (augs == null || !(true == augs.getItem(Constants.ENTITY_SKIPPED)))
        if (fLexicalHandler != null)
          fLexicalHandler.endEntity(name)
    } catch {
    case e: SAXException =>
      throw new XNIException(e)
  }

  override def startElement(
    element   : QName,
    attributes: XMLAttributes,
    augs      : Augmentations
  ): Unit =
    try {
      if (fContentHandler != null) {
        if (fNamespaces) {
          // send prefix mapping events
          startNamespaceMapping()
          // REVISIT: It should not be necessary to iterate over the attribute
          // list when the set of [namespace attributes] is empty for this
          // element. This should be computable from the NamespaceContext, but
          // since we currently don't report the mappings for the xml prefix
          // we cannot use the declared prefix count for the current context
          // to skip this section. -- mrglavas
          val len = attributes.getLength
          if (!fNamespacePrefixes)
            for (i <- len - 1 to 0 by -1) {
              attributes.setNameFields(i, fQName)
              if ((fQName.prefix eq XMLSymbols.PREFIX_XMLNS) || (fQName.rawname eq XMLSymbols.PREFIX_XMLNS)) { // remove namespace declaration attributes
                attributes.removeAttributeAt(i)
              }
            }
          else if (!fXMLNSURIs) for (i <- len - 1 to 0 by -1) {
            attributes.setNameFields(i, fQName)
            if ((fQName.prefix eq XMLSymbols.PREFIX_XMLNS) || (fQName.rawname eq XMLSymbols.PREFIX_XMLNS)) { // localpart should be empty string as per SAX documentation:
              // http://www.saxproject.org/?selected=namespaces
              fQName.prefix = ""
              fQName.uri = ""
              fQName.localpart = ""
              attributes.setName(i, fQName)
            }
          }
        }
        fAugmentations = augs
        val uri       =
          if (element.uri != null)
            element.uri
          else
            ""
        val localpart =
          if (fNamespaces)
            element.localpart
          else
            ""
        fAttributesProxy.setAttributes(attributes)
        fContentHandler.startElement(uri, localpart, element.rawname, fAttributesProxy)
      }
    } catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def characters(
    text: XMLString,
    augs: Augmentations
  ): Unit = { // if type is union (XML Schema) it is possible that we receive
    // character call with empty data
    if (text.length == 0)
      return
    try
      if (fContentHandler != null)
        fContentHandler.characters(text.ch, text.offset, text.length)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }
  }

  override def ignorableWhitespace(
    text: XMLString,
    augs: Augmentations
  ): Unit =
    try
      if (fContentHandler != null)
        fContentHandler.ignorableWhitespace(text.ch, text.offset, text.length)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def endElement(
    element: QName,
    augs   : Augmentations
  ): Unit =
    try
      if (fContentHandler != null) {
        fAugmentations = augs
        val uri =
          if (element.uri != null)
            element.uri
        else
            ""
        val localpart =
          if (fNamespaces) element
            .localpart
          else
            ""
        fContentHandler.endElement(uri, localpart, element.rawname)
        if (fNamespaces)
          endNamespaceMapping()
      }
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def startCDATA(augs: Augmentations): Unit =
    try
      if (fLexicalHandler != null)
        fLexicalHandler.startCDATA()
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def endCDATA(augs: Augmentations): Unit =
    try
      if (fLexicalHandler != null)
        fLexicalHandler.endCDATA()
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def comment(
    text: XMLString,
    augs: Augmentations
  ): Unit =
    try
      if (fLexicalHandler != null)
        fLexicalHandler.comment(text.ch, 0, text.length)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def processingInstruction(
    target: String,
    data  : XMLString,
    augs  : Augmentations
  ): Unit = {
    // REVISIT - I keep running into SAX apps that expect
    //   null data to be an empty string, which is contrary
    //   to the comment for this method in the SAX API.
    try
      if (fContentHandler != null)
        fContentHandler.processingInstruction(target, data.toString)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }
  }

  override def endDocument(augs: Augmentations): Unit =
    try
      if (fContentHandler != null)
        fContentHandler.endDocument()
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def startExternalSubset(
    identifier: XMLResourceIdentifier,
    augs      : Augmentations
  ): Unit =
    startParameterEntity("[dtd]", null, null, augs)

  override def endExternalSubset(augs: Augmentations): Unit =
    endParameterEntity("[dtd]", augs)

  override def startParameterEntity(
    name      : String,
    identifier: XMLResourceIdentifier,
    encoding  : String,
    augs      : Augmentations
  ): Unit =
    try
      if (augs != null && true == augs.getItem(Constants.ENTITY_SKIPPED))
        if (fContentHandler != null)
          fContentHandler.skippedEntity(name)
        else if (fLexicalHandler != null && fLexicalHandlerParameterEntities)
          fLexicalHandler.startEntity(name)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def endParameterEntity(
    name: String,
    augs: Augmentations
  ): Unit =
    try
      if (augs == null || !(true == augs.getItem(Constants.ENTITY_SKIPPED)))
        if (fLexicalHandler != null && fLexicalHandlerParameterEntities)
          fLexicalHandler.endEntity(name)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def elementDecl(
    name        : String,
    contentModel: String,
    augs        : Augmentations
  ): Unit =
    try
      if (fDeclHandler != null)
        fDeclHandler.elementDecl(name, contentModel)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def attributeDecl(
    elementName              : String,
    attributeName            : String,
    `type`                   : String,
    enumeration              : Array[String],
    defaultType              : String,
    defaultValue             : XMLString,
    nonNormalizedDefaultValue: XMLString,
    augs                     : Augmentations
  ): Unit =
    try
      if (fDeclHandler != null) {
        // used as a key to detect duplicate attribute definitions.
        val elemAttr = new StringBuffer(elementName).append('<').append(attributeName).toString
        if (fDeclaredAttrs.get(elemAttr) != null) {
          // we aren't permitted to return duplicate attribute definitions
          return
        }
        fDeclaredAttrs.put(elemAttr, true)
        val _type =
          if (`type` == "NOTATION" || `type` == "ENUMERATION") {
            val str = new StringBuffer
            if (`type` == "NOTATION") {
              str.append(`type`)
              str.append(" (")
            } else
              str.append('(')
            for (i <- enumeration.indices) {
              str.append(enumeration(i))
              if (i < enumeration.length - 1)
                str.append('|')
            }
            str.append(')')
            str.toString
          } else
            `type`
        val value =
          if (defaultValue == null)
            null
          else
            defaultValue.toString
        fDeclHandler.attributeDecl(elementName, attributeName, _type, defaultType, value)
      }
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def internalEntityDecl(
    name             : String,
    text             : XMLString,
    nonNormalizedText: XMLString,
    augs             : Augmentations
  ): Unit =
    try
      if (fDeclHandler != null)
        fDeclHandler.internalEntityDecl(name, text.toString)
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def externalEntityDecl(
    name      : String,
    identifier: XMLResourceIdentifier,
    augs      : Augmentations
  ): Unit =
    try
      if (fDeclHandler != null) {
        val publicId = identifier.getPublicId
        val systemId =
          if (fResolveDTDURIs)
            identifier.getExpandedSystemId
          else
            identifier.getLiteralSystemId
        fDeclHandler.externalEntityDecl(name, publicId, systemId)
      }
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def unparsedEntityDecl(
    name      : String,
    identifier: XMLResourceIdentifier,
    notation  : String,
    augs      : Augmentations
  ): Unit =
    try
      if (fDTDHandler != null) {
        val publicId = identifier.getPublicId
        val systemId =
          if (fResolveDTDURIs)
            identifier.getExpandedSystemId
          else
            identifier.getLiteralSystemId
        fDTDHandler.unparsedEntityDecl(name, publicId, systemId, notation)
      }
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def notationDecl(
    name      : String,
    identifier: XMLResourceIdentifier,
    augs      : Augmentations
  ): Unit =
    try
      if (fDTDHandler != null) {
        val publicId = identifier.getPublicId
        val systemId =
          if (fResolveDTDURIs)
            identifier.getExpandedSystemId
          else
            identifier.getLiteralSystemId
        fDTDHandler.notationDecl(name, publicId, systemId)
      }
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }

  override def endDTD(augs: Augmentations): Unit = {
    fInDTD = false
    try
      if (fLexicalHandler != null)
        fLexicalHandler.endDTD()
    catch {
      case e: SAXException =>
        throw new XNIException(e)
    }
    if (fDeclaredAttrs != null) // help out the GC
      fDeclaredAttrs.clear()
  }

  override def parse(systemId: String): Unit =
    parseAndHandleException(new XMLInputSource(null, systemId, null))

  override def parse(inputSource: InputSource): Unit = {
      val xmlInputSource = new XMLInputSource(inputSource.getPublicId, inputSource.getSystemId, null)
      xmlInputSource.setByteStream(inputSource.getByteStream)
      xmlInputSource.setCharacterStream(inputSource.getCharacterStream)
      xmlInputSource.setEncoding(inputSource.getEncoding)
      parseAndHandleException(xmlInputSource)
    }

  // ORBEON: To remove code duplication between the two original `parse()` methods.
  private def parseAndHandleException(inputSource: XMLInputSource): Unit =
    try
      parse(inputSource)
    catch {
      case e: XMLParseException =>
        e.getException match {
          case ex @ (null | _: CharConversionException) =>
            // must be a parser exception; mine it for locator info
            // and throw a SAXParseException
            val locatorImpl = new Locator2Impl
            // since XMLParseExceptions know nothing about encoding,
            // we cannot return anything meaningful in this context.
            // We *could* consult the LocatorProxy, but the
            // application can do this itself if it wishes to possibly
            // be mislead.
            locatorImpl.setXMLVersion(fVersion)
            locatorImpl.setPublicId(e.getPublicId)
            locatorImpl.setSystemId(e.getExpandedSystemId)
            locatorImpl.setLineNumber(e.getLineNumber)
            locatorImpl.setColumnNumber(e.getColumnNumber)
            throw (
              if (ex == null)
                new SAXParseException(e.getMessage, locatorImpl)
              else
                new SAXParseException(e.getMessage, locatorImpl, ex)
            )
          case e @ (_: SAXException | _: IOException) => // why did we create an XMLParseException?
            throw e
          case ex =>
            throw new SAXException(ex)
        }
      case e: XNIException =>
        e.getException match {
          case null                                   => throw new SAXException(e.getMessage)
          case e @ (_: SAXException | _: IOException) => throw e
          case ex                                     => throw new SAXException(ex)
        }
    }

  override def setEntityResolver(resolver: EntityResolver): Unit =
    try {
      val xer = fConfiguration.getProperty(XMLParser.ENTITY_RESOLVER).asInstanceOf[XMLEntityResolver]
      if (fUseEntityResolver2 && resolver.isInstanceOf[EntityResolver2])
        xer match {
          case er2w: EntityResolver2Wrapper =>
            er2w.setEntityResolver(resolver.asInstanceOf[EntityResolver2])
          case _                            =>
           fConfiguration.setProperty(XMLParser.ENTITY_RESOLVER, new EntityResolver2Wrapper(resolver.asInstanceOf[EntityResolver2]))
        }
      else xer match {
        case erw: EntityResolverWrapper =>
          erw.setEntityResolver(resolver)
        case _                          =>
        fConfiguration.setProperty(XMLParser.ENTITY_RESOLVER, new EntityResolverWrapper(resolver))
      }
    } catch {
      case _: XMLConfigurationException => // do nothing
    }

  override def getEntityResolver: EntityResolver = {
    try {
      fConfiguration.getProperty(XMLParser.ENTITY_RESOLVER) match {
        case wrapper: EntityResolverWrapper  => wrapper.getEntityResolver
        case wrapper: EntityResolver2Wrapper => wrapper.getEntityResolver
        case _                               => null
      }
    } catch {
      case _: XMLConfigurationException => null
    }
  }

  override def setErrorHandler(errorHandler: ErrorHandler): Unit =
    try {
      fConfiguration.getProperty(XMLParser.ERROR_HANDLER) match {
        case ehw: ErrorHandlerWrapper =>
          ehw.setErrorHandler(errorHandler)
        case _                        =>
          fConfiguration.setProperty(XMLParser.ERROR_HANDLER, new ErrorHandlerWrapper(errorHandler))
      }
    } catch {
      case _: XMLConfigurationException =>
    }

  override def getErrorHandler: ErrorHandler = {
    try {
      fConfiguration.getProperty(XMLParser.ERROR_HANDLER) match {
        case wrapper: ErrorHandlerWrapper => wrapper.getErrorHandler
        case _                            => null
      }
    } catch {
      case _: XMLConfigurationException => null
    }
  }

//  def setLocale(locale: Locale): Unit = {
//    //REVISIT:this methods is not part of SAX2 interfaces, we should throw exception
//    //if any application uses SAX2 and sets locale also. -nb
//    fConfiguration.setLocale(locale)
//  }

  override def setDTDHandler(dtdHandler: DTDHandler): Unit = fDTDHandler = dtdHandler
  override def setContentHandler(contentHandler: ContentHandler): Unit = fContentHandler = contentHandler
  override def getContentHandler: ContentHandler = fContentHandler
  override def getDTDHandler: DTDHandler = fDTDHandler

  override def setFeature(
    featureId: String,
    state    : Boolean
  ): Unit =
    try {
      // SAX2 Features
      if (featureId.startsWith(Constants.SAX_FEATURE_PREFIX)) {
        val suffixLength = featureId.length - Constants.SAX_FEATURE_PREFIX.length
        // http://xml.org/sax/features/namespaces
        if (suffixLength == Constants.NAMESPACES_FEATURE.length && featureId.endsWith(Constants.NAMESPACES_FEATURE)) {
          fConfiguration.setFeature(featureId, state)
          fNamespaces = state
          return
        }
        // http://xml.org/sax/features/namespace-prefixes
        //   controls the reporting of raw prefixed names and Namespace
        //   declarations (xmlns* attributes): when this feature is false
        //   (the default), raw prefixed names may optionally be reported,
        //   and xmlns* attributes must not be reported.
        if (suffixLength == Constants.NAMESPACE_PREFIXES_FEATURE.length && featureId.endsWith(Constants.NAMESPACE_PREFIXES_FEATURE)) {
          fNamespacePrefixes = state
          return
        }
        // http://xml.org/sax/features/string-interning
        //   controls the use of java.lang.String#intern() for strings
        //   passed to SAX handlers.
        if (suffixLength == Constants.STRING_INTERNING_FEATURE.length && featureId.endsWith(Constants.STRING_INTERNING_FEATURE)) {
          if (! state)
            throw new SAXNotSupportedException(
              s"False state for feature `$featureId` is not supported."
//              SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "false-not-supported", Array[AnyRef](featureId))
            )
          return
        }
        // http://xml.org/sax/features/lexical-handler/parameter-entities
        //   controls whether the beginning and end of parameter entities
        //   will be reported to the LexicalHandler.
        if (suffixLength == Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE.length && featureId.endsWith(Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE)) {
          fLexicalHandlerParameterEntities = state
          return
        }
        // http://xml.org/sax/features/resolve-dtd-uris
        //   controls whether system identifiers will be absolutized relative to
        //   their base URIs before reporting.
        if (suffixLength == Constants.RESOLVE_DTD_URIS_FEATURE.length && featureId.endsWith(Constants.RESOLVE_DTD_URIS_FEATURE)) {
          fResolveDTDURIs = state
          return
        }
        // http://xml.org/sax/features/unicode-normalization-checking
        //   controls whether Unicode normalization checking is performed
        //   as per Appendix B of the XML 1.1 specification
        if (suffixLength == Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE.length && featureId.endsWith(
          Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE
        )) { // REVISIT: Allow this feature to be set once Unicode normalization
          // checking is supported -- mrglavas.
          if (state)
            throw new SAXNotSupportedException(
              s"True state for feature `$featureId` is not supported."
//              SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "true-not-supported", Array[AnyRef](featureId))
            )
          return
        }
        // http://xml.org/sax/features/xmlns-uris
        //   controls whether the parser reports that namespace declaration
        //   attributes as being in the namespace: http://www.w3.org/2000/xmlns/
        if (suffixLength == Constants.XMLNS_URIS_FEATURE.length && featureId.endsWith(Constants.XMLNS_URIS_FEATURE)) {
          fXMLNSURIs = state
          return
        }
        // http://xml.org/sax/features/use-entity-resolver2
        //   controls whether the methods of an object implementing
        //   org.xml.sax.ext.EntityResolver2 will be used by the parser.
        if (suffixLength == Constants.USE_ENTITY_RESOLVER2_FEATURE.length && featureId.endsWith(Constants.USE_ENTITY_RESOLVER2_FEATURE)) {
          if (state != fUseEntityResolver2) {
            fUseEntityResolver2 = state
            // Refresh EntityResolver wrapper.
            setEntityResolver(getEntityResolver)
          }
          return
        }
        // Read only features.
        // http://xml.org/sax/features/is-standalone
        //   reports whether the document specified a standalone document declaration.
        // http://xml.org/sax/features/use-attributes2
        //   reports whether Attributes objects passed to startElement also implement
        //   the org.xml.sax.ext.Attributes2 interface.
        // http://xml.org/sax/features/use-locator2
        //   reports whether Locator objects passed to setDocumentLocator also implement
        //   the org.xml.sax.ext.Locator2 interface.
        // http://xml.org/sax/features/xml-1.1
        //   reports whether the parser supports both XML 1.1 and XML 1.0.
        if ((suffixLength == Constants.IS_STANDALONE_FEATURE.length && featureId.endsWith(Constants.IS_STANDALONE_FEATURE)) ||
            (suffixLength == Constants.USE_ATTRIBUTES2_FEATURE.length && featureId.endsWith(Constants.USE_ATTRIBUTES2_FEATURE)) ||
            (suffixLength == Constants.USE_LOCATOR2_FEATURE.length && featureId.endsWith(Constants.USE_LOCATOR2_FEATURE)) ||
            (suffixLength == Constants.XML_11_FEATURE.length && featureId.endsWith(Constants.XML_11_FEATURE)))
          throw new SAXNotSupportedException(
            s"Feature `$featureId` is read only."
//          SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "feature-read-only", Array[AnyRef](featureId))
        )
        // Drop through and perform default processing
      }
      // Xerces Features
      /*
      else if (featureId.startsWith(XERCES_FEATURES_PREFIX)) {
          String feature = featureId.substring(XERCES_FEATURES_PREFIX.length());
          //
          // Drop through and perform default processing
          //
      }
      */
      // Default handling
      fConfiguration.setFeature(featureId, state)
    } catch {
      case e: XMLConfigurationException =>
        val identifier = e.getIdentifier
        if (e.getType == XMLConfigurationException.NOT_RECOGNIZED)
          throw new SAXNotRecognizedException(
            s"Feature `$identifier` is not recognized."
  //          SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "feature-not-recognized", Array[AnyRef](identifier))
          )
        else
          throw new SAXNotSupportedException(
            s"Feature `$identifier` is not supported."
  //          SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "feature-not-supported", Array[AnyRef](identifier))
          )
    }

  override def getFeature(featureId: String): Boolean = try {
    if (featureId.startsWith(Constants.SAX_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.SAX_FEATURE_PREFIX.length
      if (suffixLength == Constants.NAMESPACE_PREFIXES_FEATURE.length && featureId.endsWith(Constants.NAMESPACE_PREFIXES_FEATURE))
        return fNamespacePrefixes
      if (suffixLength == Constants.STRING_INTERNING_FEATURE.length && featureId.endsWith(Constants.STRING_INTERNING_FEATURE))
        return true
      if (suffixLength == Constants.IS_STANDALONE_FEATURE.length && featureId.endsWith(Constants.IS_STANDALONE_FEATURE))
        return fStandalone
      if (suffixLength == Constants.XML_11_FEATURE.length && featureId.endsWith(Constants.XML_11_FEATURE)) {
        // ORBEON: XML 1.1 not supported.
        return false
//        return fConfiguration.isInstanceOf[XML11Configurable]
      }
      if (suffixLength == Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE.length && featureId.endsWith(Constants.LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE))
        return fLexicalHandlerParameterEntities
      if (suffixLength == Constants.RESOLVE_DTD_URIS_FEATURE.length && featureId.endsWith(Constants.RESOLVE_DTD_URIS_FEATURE))
        return fResolveDTDURIs
      if (suffixLength == Constants.XMLNS_URIS_FEATURE.length && featureId.endsWith(Constants.XMLNS_URIS_FEATURE))
        return fXMLNSURIs
      if (suffixLength == Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE.length && featureId.endsWith(Constants.UNICODE_NORMALIZATION_CHECKING_FEATURE
      ))
        return false
      if (suffixLength == Constants.USE_ENTITY_RESOLVER2_FEATURE.length && featureId.endsWith(Constants.USE_ENTITY_RESOLVER2_FEATURE))
        return fUseEntityResolver2
      if ((suffixLength == Constants.USE_ATTRIBUTES2_FEATURE.length && featureId.endsWith(Constants.USE_ATTRIBUTES2_FEATURE)) ||
          (suffixLength == Constants.USE_LOCATOR2_FEATURE.length && featureId.endsWith(Constants.USE_LOCATOR2_FEATURE)))
        return true
    }
    /*
    else if (featureId.startsWith(XERCES_FEATURES_PREFIX)) {
        //
        // Drop through and perform default processing
        //
    }
    */
    fConfiguration.getFeature(featureId)
  } catch {
    case e: XMLConfigurationException =>
      val identifier = e.getIdentifier
      if (e.getType == XMLConfigurationException.NOT_RECOGNIZED) throw new SAXNotRecognizedException(
        s"Feature `$identifier` is not recognized."
//        SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "feature-not-recognized", Array[AnyRef](identifier))
      )
      else throw new SAXNotSupportedException(
        s"Feature `$identifier` is not supported."
//        SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "feature-not-supported", Array[AnyRef](identifier))
      )
  }

  override def setProperty(
    propertyId: String,
    value     : AnyRef
  ): Unit =
    try {
      // SAX2 core properties
      if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
        val suffixLength = propertyId.length - Constants.SAX_PROPERTY_PREFIX.length
        // http://xml.org/sax/properties/lexical-handler
        // Value type: org.xml.sax.ext.LexicalHandler
        // Access: read/write, pre-parse only
        //   Set the lexical event handler.
        if (suffixLength == Constants.LEXICAL_HANDLER_PROPERTY.length && propertyId.endsWith(Constants.LEXICAL_HANDLER_PROPERTY)) {
          try
            setLexicalHandler(value.asInstanceOf[LexicalHandler])
          catch {
            case _: ClassCastException =>
              throw new SAXNotSupportedException(
                s"The value specified for property `$propertyId` cannot be casted to `org.xml.sax.ext.LexicalHandler`."
//                SAXMessageFormatter.formatMessage(
//                  fConfiguration.getLocale, "incompatible-class",
//                  Array[AnyRef](propertyId, "org.xml.sax.ext.LexicalHandler")
//                )
              )
          }
          return
        }
        // http://xml.org/sax/properties/declaration-handler
        // Value type: org.xml.sax.ext.DeclHandler
        //   Set the DTD declaration event handler.
        if (suffixLength == Constants.DECLARATION_HANDLER_PROPERTY.length && propertyId.endsWith(Constants.DECLARATION_HANDLER_PROPERTY)) {
          try
            setDeclHandler(value.asInstanceOf[DeclHandler])
          catch {
            case _: ClassCastException =>
              throw new SAXNotSupportedException(
                s"The value specified for property `$propertyId` cannot be casted to `org.xml.sax.ext.DeclHandler`."
//                SAXMessageFormatter.formatMessage(
//                  fConfiguration.getLocale, "incompatible-class", Array[AnyRef](propertyId, "org.xml.sax.ext.DeclHandler")
//                )
              )
          }
          return
        }
        // http://xml.org/sax/properties/dom-node
        // Value type: DOM Node
        // Access: read-only
        //   Get the DOM node currently being visited, if the SAX parser is
        //   iterating over a DOM tree.  If the parser recognises and
        //   supports this property but is not currently visiting a DOM
        //   node, it should return null (this is a good way to check for
        //   availability before the parse begins).
        // http://xml.org/sax/properties/document-xml-version
        // Value type: java.lang.String
        //   The literal string describing the actual XML version of the document.
        if ((suffixLength == Constants.DOM_NODE_PROPERTY.length && propertyId.endsWith(Constants.DOM_NODE_PROPERTY)) ||
            (suffixLength == Constants.DOCUMENT_XML_VERSION_PROPERTY.length && propertyId.endsWith(Constants.DOCUMENT_XML_VERSION_PROPERTY)))
          throw new SAXNotSupportedException(
            s"Property `$propertyId` is read only."
//            SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "property-read-only", Array[AnyRef](propertyId))
          )
      }
      // Xerces Properties
      /*
        else if (propertyId.startsWith(XERCES_PROPERTIES_PREFIX)) {
            //
            // Drop through and perform default processing
            //
        }
        */
      // Perform default processing
      fConfiguration.setProperty(propertyId, value)
    } catch {
      case e: XMLConfigurationException =>
        val identifier = e.getIdentifier
        if (e.getType == XMLConfigurationException.NOT_RECOGNIZED)
          throw new SAXNotRecognizedException(
            s"Property `$identifier` is not recognized."
  //          SAXMessageFormatter.formatMessage(
  //            fConfiguration.getLocale, "property-not-recognized", Array[AnyRef](identifier)
  //          )
          )
        else
          throw new SAXNotSupportedException(
            s"Property `$identifier` is not supported."
//            SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "property-not-supported", Array[AnyRef](identifier))
          )
    }

  override def getProperty(propertyId: String): AnyRef = try {
    if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.SAX_PROPERTY_PREFIX.length
      if (suffixLength == Constants.DOCUMENT_XML_VERSION_PROPERTY.length && propertyId.endsWith(Constants.DOCUMENT_XML_VERSION_PROPERTY))
        return fVersion
      if (suffixLength == Constants.LEXICAL_HANDLER_PROPERTY.length && propertyId.endsWith(Constants.LEXICAL_HANDLER_PROPERTY))
        return getLexicalHandler
      if (suffixLength == Constants.DECLARATION_HANDLER_PROPERTY.length && propertyId.endsWith(Constants.DECLARATION_HANDLER_PROPERTY))
        return getDeclHandler
      if (suffixLength == Constants.DOM_NODE_PROPERTY.length && propertyId.endsWith(Constants.DOM_NODE_PROPERTY)) {
        // we are not iterating a DOM tree
        throw new SAXNotSupportedException(
          s"Cannot read DOM node property. No DOM tree exists."
//          SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "dom-node-read-not-supported", null)
        )
      }
    }
    // Xerces properties
    fConfiguration.getProperty(propertyId)
  } catch {
    case e: XMLConfigurationException =>
      val identifier = e.getIdentifier
      if (e.getType == XMLConfigurationException.NOT_RECOGNIZED)
        throw new SAXNotRecognizedException(
          s"Property `$identifier` is not recognized."
  //        SAXMessageFormatter.formatMessage(
  //          fConfiguration.getLocale, "property-not-recognized", Array[AnyRef](identifier)
  //        )
        )
      else
        throw new SAXNotSupportedException(
          s"Property `$identifier` is not supported."
//          SAXMessageFormatter.formatMessage(fConfiguration.getLocale, "property-not-supported", Array[AnyRef](identifier))
        )
  }

  protected def setDeclHandler(handler: DeclHandler): Unit = {
    if (fParseInProgress)
      throw new SAXNotSupportedException(
        s"Property `http://xml.org/sax/properties/declaration-handler` is not supported while parsing."
//        SAXMessageFormatter.formatMessage(
//          fConfiguration.getLocale, "property-not-parsing-supported",
//          Array[AnyRef]("http://xml.org/sax/properties/declaration-handler")
//        )
      )
    fDeclHandler = handler
  }

  protected def getDeclHandler: DeclHandler = fDeclHandler

  def setLexicalHandler(handler: LexicalHandler): Unit = {
    if (fParseInProgress)
      throw new SAXNotSupportedException(
        s"Property `http://xml.org/sax/properties/lexical-handler` is not supported while parsing."
//        SAXMessageFormatter.formatMessage(
//          fConfiguration.getLocale, "property-not-parsing-supported",
//          Array[AnyRef]("http://xml.org/sax/properties/lexical-handler")
//        )
      )
    fLexicalHandler = handler
  }

  def getLexicalHandler: LexicalHandler = fLexicalHandler

  final protected def startNamespaceMapping(): Unit = {
    val count = fNamespaceContext.getDeclaredPrefixCount
    if (count > 0) {
      var prefix: String = null
      var uri   : String = null
      for (i <- 0 until count) {
        prefix = fNamespaceContext.getDeclaredPrefixAt(i)
        uri = fNamespaceContext.getURI(prefix)
        fContentHandler.startPrefixMapping(
          prefix, if (uri == null) "" else uri
        )
      }
    }
  }

  final protected def endNamespaceMapping(): Unit = {
    val count = fNamespaceContext.getDeclaredPrefixCount
    if (count > 0) for (i <- 0 until count) {
      fContentHandler.endPrefixMapping(fNamespaceContext.getDeclaredPrefixAt(i))
    }
  }

  /**
   * Reset all components before parsing.
   *
   * @throws XNIException Thrown if an error occurs during initialization.
   */
  override def reset(): Unit = {
    super.reset()

    // reset state
    fInDTD = false
    fVersion = "1.0"
    fStandalone = false

    // features
    fNamespaces = fConfiguration.getFeature(NAMESPACES)
    fAugmentations = null
    fDeclaredAttrs = null
  }
}

private object AbstractSAXParser {

  //
  // Constants
  //

  // features

  /** Feature identifier: namespaces. */
  val NAMESPACES: String = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE

  /** Feature id: string interning. */
  val STRING_INTERNING: String = Constants.SAX_FEATURE_PREFIX + Constants.STRING_INTERNING_FEATURE

  /** Feature identifier: allow notation and unparsed entity events to be sent out of order. */
  // this is not meant to be a recognized feature, but we need it here to use
  // if it is already a recognized feature for the pipeline
  val ALLOW_UE_AND_NOTATION_EVENTS: String = Constants.SAX_FEATURE_PREFIX + Constants.ALLOW_DTD_EVENTS_AFTER_ENDDTD_FEATURE

  /** Recognized features. */
  val RECOGNIZED_FEATURES = Array(NAMESPACES, STRING_INTERNING)

  // properties

  /** Property id: lexical handler. */
  val LEXICAL_HANDLER: String = Constants.SAX_PROPERTY_PREFIX + Constants.LEXICAL_HANDLER_PROPERTY

  /** Property id: declaration handler. */
  val DECLARATION_HANDLER: String = Constants.SAX_PROPERTY_PREFIX + Constants.DECLARATION_HANDLER_PROPERTY

  /** Property id: DOM node. */
  val DOM_NODE: String = Constants.SAX_PROPERTY_PREFIX + Constants.DOM_NODE_PROPERTY

  /** Recognized properties. */
  val RECOGNIZED_PROPERTIES = Array(LEXICAL_HANDLER, DECLARATION_HANDLER, DOM_NODE)

  //
  // Classes
  //
  final protected class LocatorProxy(var fLocator: XMLLocator)
    extends Locator2 {

    // Locator methods
    def getPublicId: String = fLocator.getPublicId
    def getSystemId: String = fLocator.getExpandedSystemId
    def getLineNumber: Int = fLocator.getLineNumber
    def getColumnNumber: Int = fLocator.getColumnNumber

    // Locator2 methods
    def getXMLVersion: String = fLocator.getXMLVersion
    def getEncoding: String = fLocator.getEncoding
  }

  final protected class AttributesProxy extends Attributes2 {

    protected var fAttributes: XMLAttributes = null

    // Public methods
    def setAttributes(attributes: XMLAttributes): Unit =
      fAttributes = attributes

    // setAttributes(XMLAttributes)}
    def getLength: Int = fAttributes.getLength

    def getQName(index: Int): String = fAttributes.getQName(index)

    def getURI(index: Int): String = {
      // REVISIT: this hides the fact that internally we use
      //          null instead of empty string
      //          SAX requires URI to be a string or an empty string
      val uri = fAttributes.getURI(index)
      if (uri != null)
        uri
      else
        ""
    }

    def getLocalName(index: Int): String = fAttributes.getLocalName(index)

    def getType(i: Int): String = fAttributes.getType(i)

    def getType(name: String): String = fAttributes.getType(name)

    def getType(uri: String, localName: String): String =
      if (uri.isEmpty)
        fAttributes.getType(null, localName)
      else
        fAttributes.getType(uri, localName)

    def getValue(i: Int): String = fAttributes.getValue(i)

    def getValue(name: String): String = fAttributes.getValue(name)

    def getValue(uri: String, localName: String): String =
      if (uri.isEmpty)
        fAttributes.getValue(null, localName)
      else
        fAttributes.getValue(uri, localName)

    def getIndex(qName: String): Int = fAttributes.getIndex(qName)

    def getIndex(uri: String, localPart: String): Int =
      if (uri.isEmpty)
        fAttributes.getIndex(null, localPart)
      else
        fAttributes.getIndex(uri, localPart)

    // Attributes2 methods
    // REVISIT: Localize exception messages. -- mrglavas
    def isDeclared(index: Int): Boolean = {
      if (index < 0 || index >= fAttributes.getLength)
        throw new ArrayIndexOutOfBoundsException(index)
      fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED) == true
    }

    def isDeclared(qName: String): Boolean = {
      val index = getIndex(qName)
      if (index == -1)
        throw new IllegalArgumentException(qName)
      fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED) == true
    }

    def isDeclared(uri: String, localName: String): Boolean = {
      val index = getIndex(uri, localName)
      if (index == -1)
        throw new IllegalArgumentException(localName)
      fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED) == true
    }

    def isSpecified(index: Int): Boolean = {
      if (index < 0 || index >= fAttributes.getLength)
        throw new ArrayIndexOutOfBoundsException(index)
      fAttributes.isSpecified(index)
    }

    def isSpecified(qName: String): Boolean = {
      val index = getIndex(qName)
      if (index == -1)
        throw new IllegalArgumentException(qName)
      fAttributes.isSpecified(index)
    }

    def isSpecified(uri: String, localName: String): Boolean = {
      val index = getIndex(uri, localName)
      if (index == -1)
        throw new IllegalArgumentException(localName)
      fAttributes.isSpecified(index)
    }
  }
}