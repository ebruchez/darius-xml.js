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

package org.orbeon.darius.xml.xni

import org.orbeon.darius.xml.xni.parser.XMLDocumentSource

/**
 * The document handler interface defines callback methods to report
 * information items in XML documents. Parser components interested in
 * document information implement this interface and are registered
 * as the document handler on the document source.
 */
trait XMLDocumentHandler {

  /**
   * The start of the document.
   *
   * @param locator  The document locator, or null if the document
   *                 location cannot be reported during the parsing
   *                 of this document. However, it is *strongly*
   *                 recommended that a locator be supplied that can
   *                 at least report the system identifier of the
   *                 document.
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
   *
   * @param augs     Additional information that may include infoset augmentations
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def startDocument(
    locator          : XMLLocator, 
    encoding         : String, 
    namespaceContext : NamespaceContext, 
    augs             : Augmentations
  ): Unit

  /**
   * Notifies of the presence of an XMLDecl line in the document. If
   * present, this method will be called immediately following the
   * startDocument call.
   *
   * @param version    The XML version.
   * @param encoding   The IANA encoding name of the document, or null if
   *                   not specified.
   * @param standalone The standalone value, or null if not specified.
   * @param augs       Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def xmlDecl(
    version    : String, 
    encoding   : String, 
    standalone : String, 
    augs       : Augmentations
  ): Unit

  /**
   * Notifies of the presence of the DOCTYPE line in the document.
   *
   * @param rootElement
   *                 The name of the root element.
   * @param publicId The public identifier if an external DTD or null
   *                 if the external DTD is specified using SYSTEM.
   * @param systemId The system identifier if an external DTD, null
   *                 otherwise.
   * @param augs     Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def doctypeDecl(
    rootElement : String, 
    publicId    : String, 
    systemId    : String, 
    augs        : Augmentations
  ): Unit

  /**
   * A comment.
   *
   * @param text   The text in the comment.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by application to signal an error.
   */
  def comment(text: XMLString, augs: Augmentations): Unit

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
  def processingInstruction(target: String, data: XMLString, augs: Augmentations): Unit

  /**
   * The start of an element.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augs       Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def startElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit

  /**
   * An empty element.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augs       Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def emptyElement(element: QName, attributes: XMLAttributes, augs: Augmentations): Unit

  /**
   * This method notifies the start of a general entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
   *
   * @param name     The name of the general entity.
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
      augs: Augmentations): Unit

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
  def textDecl(version: String, encoding: String, augs: Augmentations): Unit

  /**
   * This method notifies the end of a general entity.
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
  def endGeneralEntity(name: String, augs: Augmentations): Unit

  /**
   * Character content.
   *
   * @param text   The content.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def characters(text: XMLString, augs: Augmentations): Unit

  /**
   * Ignorable whitespace. For this method to be called, the document
   * source must have some way of determining that the text containing
   * only whitespace characters should be considered ignorable. For
   * example, the validator can determine if a length of whitespace
   * characters in the document are ignorable based on the element
   * content model.
   *
   * @param text   The ignorable whitespace.
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def ignorableWhitespace(text: XMLString, augs: Augmentations): Unit

  /**
   * The end of an element.
   *
   * @param element The name of the element.
   * @param augs    Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def endElement(element: QName, augs: Augmentations): Unit

  /**
   * The start of a CDATA section.
   *
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def startCDATA(augs: Augmentations): Unit

  /**
   * The end of a CDATA section.
   *
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def endCDATA(augs: Augmentations): Unit

  /**
   * The end of the document.
   *
   * @param augs   Additional information that may include infoset augmentations
   *
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def endDocument(augs: Augmentations): Unit

  /**
   Sets the document source.
   */
  def setDocumentSource(source: XMLDocumentSource): Unit

  /**
   Returns the document source.
   */
  def getDocumentSource: XMLDocumentSource
}
