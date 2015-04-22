package org.apache.xerces.xni

/**
 * This handler interface contains methods necessary to receive
 * information about document elements and content.
 * 
 * *Note:* Some of these methods overlap methods
 * found in the XMLDocumentHandler interface.
 *
 * @see XMLDocumentHandler
 */
trait XMLDocumentFragmentHandler {

  /**
   * The start of the document fragment.
   *
   * @param locator          The document locator, or null if the
   *                         document location cannot be reported
   *                         during the parsing of this fragment.
   *                         However, it is *strongly*
   *                         recommended that a locator be supplied
   *                         that can at least report the base
   *                         system identifier.
   * @param namespaceContext The namespace context in effect at the
   *                         start of this document fragment. This
   *                         object only represents the current context.
   *                         Implementors of this class are responsible
   *                         for copying the namespace bindings from the
   *                         the current context (and its parent contexts)
   *                         if that information is important.
   * @param augmentations    Additional information that may include infoset
   *                         augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startDocumentFragment(locator: XMLLocator, namespaceContext: NamespaceContext, augmentations: Augmentations): Unit

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
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startGeneralEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augmentations: Augmentations): Unit

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
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def textDecl(version: String, encoding: String, augmentations: Augmentations): Unit

  /**
   * This method notifies the end of a general entity.
   * 
   * *Note:* This method is not called for entity references
   * appearing as part of attribute values.
   *
   * @param name The name of the general entity.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endGeneralEntity(name: String, augmentations: Augmentations): Unit

  /**
   * A comment.
   *
   * @param text The text in the comment.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by application to signal an error.
   */
  def comment(text: XMLString, augmentations: Augmentations): Unit

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
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def processingInstruction(target: String, data: XMLString, augmentations: Augmentations): Unit

  /**
   * The start of an element.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startElement(element: QName, attributes: XMLAttributes, augmentations: Augmentations): Unit

  /**
   * An empty element.
   *
   * @param element    The name of the element.
   * @param attributes The element attributes.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def emptyElement(element: QName, attributes: XMLAttributes, augmentations: Augmentations): Unit

  /**
   * Character content.
   *
   * @param text The content.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def characters(text: XMLString, augmentations: Augmentations): Unit

  /**
   * Ignorable whitespace. For this method to be called, the document
   * source must have some way of determining that the text containing
   * only whitespace characters should be considered ignorable. For
   * example, the validator can determine if a length of whitespace
   * characters in the document are ignorable based on the element
   * content model.
   *
   * @param text The ignorable whitespace.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def ignorableWhitespace(text: XMLString, augmentations: Augmentations): Unit

  /**
   * The end of an element.
   *
   * @param element The name of the element.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endElement(element: QName, augmentations: Augmentations): Unit

  /**
   * The start of a CDATA section.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startCDATA(augmentations: Augmentations): Unit

  /**
   * The end of a CDATA section.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endCDATA(augmentations: Augmentations): Unit

  /**
   * The end of the document fragment.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endDocumentFragment(augmentations: Augmentations): Unit
}
