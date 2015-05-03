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

import org.orbeon.darius.xml.xni.parser.XMLDTDContentModelSource

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
   */
  val OCCURS_ONE_OR_MORE: Short = 4
}

/**
 * The DTD content model handler interface defines callback methods
 * to report information items in DTD content models of an element
 * declaration. Parser components interested in DTD content model
 * information implement this interface and are registered as the DTD
 * content model handler on the DTD content model source.
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
   */
  def any(augmentations: Augmentations): Unit

  /**
   * A content model of EMPTY.
   *
   * @throws XNIException Thrown by handler to signal an error.
   *
   * @param augmentations Additional information that may include infoset
   *                      augmentations.
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
