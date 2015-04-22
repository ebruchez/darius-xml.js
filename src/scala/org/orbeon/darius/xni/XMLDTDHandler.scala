package org.orbeon.darius.xni

import org.orbeon.darius.xni.parser.XMLDTDSource

object XMLDTDHandler {

  /**
   * Conditional section: INCLUDE.
   *
   * @see #CONDITIONAL_IGNORE
   */
  val CONDITIONAL_INCLUDE: Short = 0

  /**
   * Conditional section: IGNORE.
   *
   * @see #CONDITIONAL_INCLUDE
   */
  val CONDITIONAL_IGNORE: Short = 1
}

/**
 * The DTD handler interface defines callback methods to report
 * information items in the DTD of an XML document. Parser components
 * interested in DTD information implement this interface and are
 * registered as the DTD handler on the DTD source.
 *
 * @see XMLDTDContentModelHandler
 */
trait XMLDTDHandler {

  /**
   * The start of the DTD.
   *
   * @param locator  The document locator, or null if the document
   *                 location cannot be reported during the parsing of
   *                 the document DTD. However, it is *strongly*
   *                 recommended that a locator be supplied that can
   *                 at least report the base system identifier of the
   *                 DTD.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startDTD(locator: XMLLocator, augmentations: Augmentations): Unit

  /**
   * This method notifies of the start of a parameter entity. The parameter
   * entity name start with a '%' character.
   *
   * @param name     The name of the parameter entity.
   * @param identifier The resource identifier.
   * @param encoding The auto-detected IANA encoding name of the entity
   *                 stream. This value will be null in those situations
   *                 where the entity encoding is not auto-detected (e.g.
   *                 internal parameter entities).
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startParameterEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augmentations: Augmentations): Unit

  /**
   * Notifies of the presence of a TextDecl line in an entity. If present,
   * this method will be called immediately following the startEntity call.
   * 
   * *Note:* This method is only called for external
   * parameter entities referenced in the DTD.
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
   * This method notifies the end of a parameter entity. Parameter entity
   * names begin with a '%' character.
   *
   * @param name The name of the parameter entity.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endParameterEntity(name: String, augmentations: Augmentations): Unit

  /**
   * The start of the DTD external subset.
   *
   * @param identifier The resource identifier.
   * @param augmentations
   *                   Additional information that may include infoset
   *                   augmentations.
   * @throws XNIException
   *                   Thrown by handler to signal an error.
   */
  def startExternalSubset(identifier: XMLResourceIdentifier, augmentations: Augmentations): Unit

  /**
   * The end of the DTD external subset.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endExternalSubset(augmentations: Augmentations): Unit

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
   * An element declaration.
   *
   * @param name         The name of the element.
   * @param contentModel The element content model.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def elementDecl(name: String, contentModel: String, augmentations: Augmentations): Unit

  /**
   * The start of an attribute list.
   *
   * @param elementName The name of the element that this attribute
   *                    list is associated with.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startAttlist(elementName: String, augmentations: Augmentations): Unit

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
   * @param augmentations Additional information that may include infoset
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
      augmentations: Augmentations): Unit

  /**
   * The end of an attribute list.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endAttlist(augmentations: Augmentations): Unit

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
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def internalEntityDecl(name: String, 
      text: XMLString, 
      nonNormalizedText: XMLString, 
      augmentations: Augmentations): Unit

  /**
   * An external entity declaration.
   *
   * @param name     The name of the entity. Parameter entity names start
   *                 with '%', whereas the name of a general entity is just
   *                 the entity name.
   * @param identifier    An object containing all location information
   *                      pertinent to this external entity.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def externalEntityDecl(name: String, identifier: XMLResourceIdentifier, augmentations: Augmentations): Unit

  /**
   * An unparsed entity declaration.
   *
   * @param name     The name of the entity.
   * @param identifier    An object containing all location information
   *                      pertinent to this unparsed entity declaration.
   * @param notation The name of the notation.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def unparsedEntityDecl(name: String, 
      identifier: XMLResourceIdentifier, 
      notation: String, 
      augmentations: Augmentations): Unit

  /**
   * A notation declaration
   *
   * @param name     The name of the notation.
   * @param identifier    An object containing all location information
   *                      pertinent to this notation.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def notationDecl(name: String, identifier: XMLResourceIdentifier, augmentations: Augmentations): Unit

  /**
   * The start of a conditional section.
   *
   * @param type The type of the conditional section. This value will
   *             either be CONDITIONAL_INCLUDE or CONDITIONAL_IGNORE.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #CONDITIONAL_INCLUDE
   * @see #CONDITIONAL_IGNORE
   */
  def startConditional(`type`: Short, augmentations: Augmentations): Unit

  /**
   * Characters within an IGNORE conditional section.
   *
   * @param text The ignored text.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def ignoredCharacters(text: XMLString, augmentations: Augmentations): Unit

  /**
   * The end of a conditional section.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endConditional(augmentations: Augmentations): Unit

  /**
   * The end of the DTD.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endDTD(augmentations: Augmentations): Unit

  def setDTDSource(source: XMLDTDSource): Unit

  def getDTDSource: XMLDTDSource
}
