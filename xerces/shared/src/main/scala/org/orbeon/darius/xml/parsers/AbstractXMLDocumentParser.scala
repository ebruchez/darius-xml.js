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

package org.orbeon.darius.xml.parsers

import org.orbeon.darius.xml.xni.Augmentations
import org.orbeon.darius.xml.xni.NamespaceContext
import org.orbeon.darius.xml.xni.QName
import org.orbeon.darius.xml.xni.XMLAttributes
import org.orbeon.darius.xml.xni.XMLDTDContentModelHandler
import org.orbeon.darius.xml.xni.XMLDTDHandler
import org.orbeon.darius.xml.xni.XMLDocumentHandler
import org.orbeon.darius.xml.xni.XMLLocator
import org.orbeon.darius.xml.xni.XMLResourceIdentifier
import org.orbeon.darius.xml.xni.XMLString
import org.orbeon.darius.xml.xni.XNIException
import org.orbeon.darius.xml.xni.parser.XMLDTDContentModelSource
import org.orbeon.darius.xml.xni.parser.XMLDTDSource
import org.orbeon.darius.xml.xni.parser.XMLDocumentSource
import org.orbeon.darius.xml.xni.parser.XMLParserConfiguration
import org.orbeon.darius.xml.xni._
import org.orbeon.darius.xml.xni.parser.XMLDTDContentModelSource
import org.orbeon.darius.xml.xni.parser.XMLDTDSource
import org.orbeon.darius.xml.xni.parser.XMLDocumentSource
import org.orbeon.darius.xml.xni.parser.XMLParserConfiguration

/**
 * This is the base class for all XML document parsers. XMLDocumentParser
 * provides a common implementation shared by the various document parsers
 * in the Xerces package. While this class is provided for convenience, it
 * does not prevent other kinds of parsers to be constructed using the XNI
 * interfaces.
 */
abstract class AbstractXMLDocumentParser protected (config: XMLParserConfiguration)
    extends XMLParser(config) with XMLDocumentHandler with XMLDTDHandler with XMLDTDContentModelHandler {

  /**
   True if inside DTD.
   */
  protected var fInDTD: Boolean = _

  /**
   Document source
   */
  protected var fDocumentSource: XMLDocumentSource = _

  /**
   DTD source
   */
  protected var fDTDSource: XMLDTDSource = _

  /**
   DTD content model source
   */
  protected var fDTDContentModelSource: XMLDTDContentModelSource = _

  config.setDocumentHandler(this)

  config.setDTDHandler(this)

  config.setDTDContentModelHandler(this)

  /**
   * The start of the document.
   *
   * @param locator The system identifier of the entity if the entity
   *                 is external, null otherwise.
   * @param encoding The auto-detected IANA encoding name of the entity
   *                 stream. This value will be null in those situations
   *                 where the entity encoding is not auto-detected (e.g.
   *                 internal entities or a document entity that is
   *                 parsed from a java.io.Reader).
   * @param namespaceContext
   *                 The namespace context in effect at the
   *                 start of this document.
   *                 This object represents the current context.
   *                 Implementors of this class are responsible
   *                 for copying the namespace bindings from the
   *                 the current context (and its parent contexts)
   *                 if that information is important.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startDocument(locator: XMLLocator, 
      encoding: String, 
      namespaceContext: NamespaceContext, 
      augs: Augmentations): Unit = {
  }

  /**
   * Notifies of the presence of an XMLDecl line in the document. If
   * present, this method will be called immediately following the
   * startDocument call.
   *
   * @param version    The XML version.
   * @param encoding   The IANA encoding name of the document, or null if
   *                   not specified.
   * @param standalone The standalone value, or null if not specified.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def xmlDecl(version: String, 
      encoding: String, 
      standalone: String, 
      augs: Augmentations): Unit = {
  }

  /**
   * Notifies of the presence of the DOCTYPE line in the document.
   *
   * @param rootElement The name of the root element.
   * @param publicId    The public identifier if an external DTD or null
   *                    if the external DTD is specified using SYSTEM.
   * @param systemId    The system identifier if an external DTD, null
   * @param augs   Additional information that may include infoset augmentations
   *                    otherwise.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def doctypeDecl(rootElement: String, 
      publicId: String, 
      systemId: String, 
      augs: Augmentations): Unit = {
  }

  /**
   * The start of an element. If the document specifies the start element
   * by using an empty tag, then the startElement method will immediately
   * be followed by the endElement method, with no intervening methods.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit = {
  }

  /**
   * An empty element.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def emptyElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit = {
    startElement(element, attributes, augs)
    endElement(element, augs)
  }

  /**
   * Character content.
   *
   * @param text The content.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def characters(text: XMLString, augs: Augmentations): Unit = {
  }

  /**
   * Ignorable whitespace. For this method to be called, the document
   * source must have some way of determining that the text containing
   * only whitespace characters should be considered ignorable. For
   * example, the validator can determine if a length of whitespace
   * characters in the document are ignorable based on the element
   * content model.
   *
   * @param text The ignorable whitespace.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def ignorableWhitespace(text: XMLString, augs: Augmentations): Unit = {
  }

  /**
   * The end of an element.
   *
   * @param element The name of the element.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endElement(element: QName, augs: Augmentations): Unit = {
  }

  /**
   * The start of a CDATA section.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startCDATA(augs: Augmentations): Unit = {
  }

  /**
   * The end of a CDATA section.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endCDATA(augs: Augmentations): Unit = {
  }

  /**
   * The end of the document.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endDocument(augs: Augmentations): Unit = {
  }

  /**
   * This method notifies the start of an entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
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
  def startGeneralEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augs: Augmentations): Unit = {
  }

  /**
   * Notifies of the presence of a TextDecl line in an entity. If present,
   * this method will be called immediately following the startEntity call.
   * 
   * *Note:* This method will never be called for the
   * document entity; it is only called for external general entities
   * referenced in document content.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
   *
   * @param version  The XML version, or null if not specified.
   * @param encoding The IANA encoding name of the entity.
   * @param augs     Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def textDecl(version: String, encoding: String, augs: Augmentations): Unit = {
  }

  /**
   * This method notifies the end of an entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
   *
   * @param name   The name of the entity.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def endGeneralEntity(name: String, augs: Augmentations): Unit = {
  }

  /**
   * A comment.
   *
   * @param text   The text in the comment.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by application to signal an error.
   */
  def comment(text: XMLString, augs: Augmentations): Unit = {
  }

  /**
   * A processing instruction. Processing instructions consist of a
   * target name and, optionally, text data. The data is only meaningful
   * to the application.
   * 
   * Typically, a processing instruction's data will contain a series
   * of pseudo-attributes. These pseudo-attributes follow the form of
   * element attributes but are *not* parsed or presented
   * to the application as anything other than text. The application is
   * responsible for parsing the data.
   *
   * @param target The target.
   * @param data   The data or null if none specified.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def processingInstruction(target: String, data: XMLString, augs: Augmentations): Unit = {
  }

  /**
   Sets the document source
   */
  def setDocumentSource(source: XMLDocumentSource): Unit = {
    fDocumentSource = source
  }

  /**
   Returns the document source
   */
  def getDocumentSource: XMLDocumentSource = fDocumentSource

  /**
   * The start of the DTD.
   *
   * @param locator  The document locator, or null if the document
   *                 location cannot be reported during the parsing of
   *                 the document DTD. However, it is *strongly*
   *                 recommended that a locator be supplied that can
   *                 at least report the base system identifier of the
   *                 DTD.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startDTD(locator: XMLLocator, augs: Augmentations): Unit = {
    fInDTD = true
  }

  /**
   * The start of the DTD external subset.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startExternalSubset(identifier: XMLResourceIdentifier, augmentations: Augmentations): Unit = {
  }

  /**
   * The end of the DTD external subset.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endExternalSubset(augmentations: Augmentations): Unit = {
  }

  /**
   * This method notifies the start of an entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
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
  def startParameterEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augs: Augmentations): Unit = {
  }

  /**
   * This method notifies the end of an entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
   *
   * @param name   The name of the entity.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def endParameterEntity(name: String, augs: Augmentations): Unit = {
  }

  /**
   * Characters within an IGNORE conditional section.
   *
   * @param text The ignored text.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def ignoredCharacters(text: XMLString, augs: Augmentations): Unit = {
  }

  /**
   * An element declaration.
   *
   * @param name         The name of the element.
   * @param contentModel The element content model.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def elementDecl(name: String, contentModel: String, augs: Augmentations): Unit = {
  }

  /**
   * The start of an attribute list.
   *
   * @param elementName The name of the element that this attribute
   *                    list is associated with.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startAttlist(elementName: String, augs: Augmentations): Unit = {
  }

  /**
   * An attribute declaration.
   *
   * @param elementName   The name of the element that this attribute
   *                      is associated with.
   * @param attributeName The name of the attribute.
   * @param type          The attribute type. This value will be one of
   *                      the following: "CDATA", "ENTITY", "ENTITIES",
   *                      "ENUMERATION", "ID", "IDREF", "IDREFS",
   *                      "NMTOKEN", "NMTOKENS", or "NOTATION".
   * @param enumeration   If the type has the value "ENUMERATION" or
   *                      "NOTATION", this array holds the allowed attribute
   *                      values; otherwise, this array is null.
   * @param defaultType   The attribute default type. This value will be
   *                      one of the following: "#FIXED", "#IMPLIED",
   *                      "#REQUIRED", or null.
   * @param defaultValue  The attribute default value, or null if no
   *                      default value is specified.
   * @param nonNormalizedDefaultValue  The attribute default value with no normalization
   *                      performed, or null if no default value is specified.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def attributeDecl(elementName: String, 
      attributeName: String, 
      `type`: String, 
      enumeration: Array[String], 
      defaultType: String, 
      defaultValue: XMLString, 
      nonNormalizedDefaultValue: XMLString, 
      augs: Augmentations): Unit = {
  }

  /**
   * The end of an attribute list.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endAttlist(augs: Augmentations): Unit = {
  }

  /**
   * An internal entity declaration.
   *
   * @param name The name of the entity. Parameter entity names start with
   *             '%', whereas the name of a general entity is just the
   *             entity name.
   * @param text The value of the entity.
   * @param nonNormalizedText The non-normalized value of the entity. This
   *             value contains the same sequence of characters that was in
   *             the internal entity declaration, without any entity
   *             references expanded.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def internalEntityDecl(name: String, 
      text: XMLString, 
      nonNormalizedText: XMLString, 
      augs: Augmentations): Unit = {
  }

  /**
   * An external entity declaration.
   *
   * @param name     The name of the entity. Parameter entity names start
   *                 with '%', whereas the name of a general entity is just
   *                 the entity name.
   * @param identifier    An object containing all location information
   *                      pertinent to this entity.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def externalEntityDecl(name: String, identifier: XMLResourceIdentifier, augs: Augmentations): Unit = {
  }

  /**
   * An unparsed entity declaration.
   *
   * @param name     The name of the entity.
   * @param identifier    An object containing all location information
   *                      pertinent to this entity.
   * @param notation The name of the notation.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def unparsedEntityDecl(name: String, 
      identifier: XMLResourceIdentifier, 
      notation: String, 
      augs: Augmentations): Unit = {
  }

  /**
   * A notation declaration
   *
   * @param name     The name of the notation.
   * @param identifier    An object containing all location information
   *                      pertinent to this notation.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def notationDecl(name: String, identifier: XMLResourceIdentifier, augs: Augmentations): Unit = {
  }

  /**
   * The start of a conditional section.
   *
   * @param type The type of the conditional section. This value will
   *             either be CONDITIONAL_INCLUDE or CONDITIONAL_IGNORE.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startConditional(`type`: Short, augs: Augmentations): Unit = {
  }

  /**
   * The end of a conditional section.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endConditional(augs: Augmentations): Unit = {
  }

  /**
   * The end of the DTD.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endDTD(augs: Augmentations): Unit = {
    fInDTD = false
  }

  def setDTDSource(source: XMLDTDSource): Unit = {
    fDTDSource = source
  }

  def getDTDSource: XMLDTDSource = fDTDSource

  /**
   * The start of a content model. Depending on the type of the content
   * model, specific methods may be called between the call to the
   * startContentModel method and the call to the endContentModel method.
   *
   * @param elementName The name of the element.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startContentModel(elementName: String, augs: Augmentations): Unit = {
  }

  /**
   * A content model of ANY.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def any(augs: Augmentations): Unit = {
  }

  /**
   * A content model of EMPTY.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def empty(augs: Augmentations): Unit = {
  }

  /**
   * A start of either a mixed or children content model. A mixed
   * content model will immediately be followed by a call to the
   * `pcdata()` method. A children content model will
   * contain additional groups and/or elements.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startGroup(augs: Augmentations): Unit = {
  }

  /**
   * The appearance of "#PCDATA" within a group signifying a
   * mixed content model. This method will be the first called
   * following the content model's `startGroup()`.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def pcdata(augs: Augmentations): Unit = {
  }

  /**
   * A referenced element in a mixed or children content model.
   *
   * @param elementName The name of the referenced element.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def element(elementName: String, augs: Augmentations): Unit = {
  }

  /**
   * The separator between choices or sequences of a mixed or children
   * content model.
   *
   * @param separator The type of children separator.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def separator(separator: Short, augs: Augmentations): Unit = {
  }

  /**
   * The occurrence count for a child in a children content model or
   * for the mixed content model group.
   *
   * @param occurrence The occurrence count for the last element
   *                   or group.
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def occurrence(occurrence: Short, augs: Augmentations): Unit = {
  }

  /**
   * The end of a group for mixed or children content models.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endGroup(augs: Augmentations): Unit = {
  }

  /**
   * The end of a content model.
   *
   * @param augs Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endContentModel(augs: Augmentations): Unit = {
  }

  def setDTDContentModelSource(source: XMLDTDContentModelSource): Unit = {
    fDTDContentModelSource = source
  }

  def getDTDContentModelSource: XMLDTDContentModelSource = fDTDContentModelSource

  /**
   * reset all components before parsing
   */
  override protected def reset(): Unit = {
    super.reset()
    fInDTD = false
  }
}
