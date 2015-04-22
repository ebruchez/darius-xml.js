package org.apache.xerces.xni

import org.apache.xerces.xni.parser.XMLDTDContentModelSource

object XMLDTDContentModelHandler {

  /**
   * A choice separator for children and mixed content models. This
   * separator is used to specify that the allowed child is one of a
   * collection.
   * 
   * For example:
   * 
   *  <!ELEMENT elem (foo|bar)>
   *  <!ELEMENT elem (foo|bar+)>
   *  <!ELEMENT elem (foo|bar|baz)>
   *  <!ELEMENT elem (#PCDATA|foo|bar)*>
   *
   * @see #SEPARATOR_SEQUENCE
   */
  val SEPARATOR_CHOICE: Short = 0

  /**
   * A sequence separator for children content models. This separator
   * is used to specify that the allowed children must follow in the
   * specified sequence.
   * 
   *  <!ELEMENT elem (foo,bar)>
   *  <!ELEMENT elem (foo,bar*)>
   *  <!ELEMENT elem (foo,bar,baz)>
   *
   * @see #SEPARATOR_CHOICE
   */
  val SEPARATOR_SEQUENCE: Short = 1

  /**
   * This occurrence count limits the element, choice, or sequence in a
   * children content model to zero or one. In other words, the child
   * is optional.
   * 
   * For example:
   * 
   *  <!ELEMENT elem (foo?)>
   *
   * @see #OCCURS_ZERO_OR_MORE
   * @see #OCCURS_ONE_OR_MORE
   */
  val OCCURS_ZERO_OR_ONE: Short = 2

  /**
   * This occurrence count limits the element, choice, or sequence in a
   * children content model to zero or more. In other words, the child
   * may appear an arbitrary number of times, or not at all. This
   * occurrence count is also used for mixed content models.
   * 
   * For example:
   * 
   *  <!ELEMENT elem (foo*)>
   *  <!ELEMENT elem (#PCDATA|foo|bar)*>
   *
   * @see #OCCURS_ZERO_OR_ONE
   * @see #OCCURS_ONE_OR_MORE
   */
  val OCCURS_ZERO_OR_MORE: Short = 3

  /**
   * This occurrence count limits the element, choice, or sequence in a
   * children content model to one or more. In other words, the child
   * may appear an arbitrary number of times, but must appear at least
   * once.
   * 
   * For example:
   * 
   *  <!ELEMENT elem (foo+)>
   *
   * @see #OCCURS_ZERO_OR_ONE
   * @see #OCCURS_ZERO_OR_MORE
   */
  val OCCURS_ONE_OR_MORE: Short = 4
}

/**
 * The DTD content model handler interface defines callback methods
 * to report information items in DTD content models of an element
 * declaration. Parser components interested in DTD content model
 * information implement this interface and are registered as the DTD
 * content model handler on the DTD content model source.
 *
 * @see XMLDTDHandler
 */
trait XMLDTDContentModelHandler {

  /**
   * The start of a content model. Depending on the type of the content
   * model, specific methods may be called between the call to the
   * startContentModel method and the call to the endContentModel method.
   *
   * @param elementName The name of the element.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startContentModel(elementName: String, augmentations: Augmentations): Unit

  /**
   * A content model of ANY.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #empty
   * @see #startGroup
   */
  def any(augmentations: Augmentations): Unit

  /**
   * A content model of EMPTY.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @see #any
   * @see #startGroup
   */
  def empty(augmentations: Augmentations): Unit

  /**
   * A start of either a mixed or children content model. A mixed
   * content model will immediately be followed by a call to the
   * `pcdata()` method. A children content model will
   * contain additional groups and/or elements.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #any
   * @see #empty
   */
  def startGroup(augmentations: Augmentations): Unit

  /**
   * The appearance of "#PCDATA" within a group signifying a
   * mixed content model. This method will be the first called
   * following the content model's `startGroup()`.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #startGroup
   */
  def pcdata(augmentations: Augmentations): Unit

  /**
   * A referenced element in a mixed or children content model.
   *
   * @param elementName The name of the referenced element.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def element(elementName: String, augmentations: Augmentations): Unit

  /**
   * The separator between choices or sequences of a mixed or children
   * content model.
   *
   * @param separator The type of children separator.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #SEPARATOR_CHOICE
   * @see #SEPARATOR_SEQUENCE
   */
  def separator(separator: Short, augmentations: Augmentations): Unit

  /**
   * The occurrence count for a child in a children content model or
   * for the mixed content model group.
   *
   * @param occurrence The occurrence count for the last element
   *                   or group.
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @see #OCCURS_ZERO_OR_ONE
   * @see #OCCURS_ZERO_OR_MORE
   * @see #OCCURS_ONE_OR_MORE
   */
  def occurrence(occurrence: Short, augmentations: Augmentations): Unit

  /**
   * The end of a group for mixed or children content models.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endGroup(augmentations: Augmentations): Unit

  /**
   * The end of a content model.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endContentModel(augmentations: Augmentations): Unit

  def setDTDContentModelSource(source: XMLDTDContentModelSource): Unit

  def getDTDContentModelSource: XMLDTDContentModelSource
}
